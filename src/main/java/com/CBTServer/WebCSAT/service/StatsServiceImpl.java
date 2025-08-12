package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.repository.StatsRepository;
import com.CBTServer.WebCSAT.repository.proj.BinRow;
import com.CBTServer.WebCSAT.repository.proj.CohortCountRow;
import com.CBTServer.WebCSAT.repository.proj.UserLatestAttemptRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repo;

    // ------- 사용자 트렌드 -------
    @Override
    public List<Map<String, Object>> getPercentileTrend(Long userId, String groupBy) {
        List<UserLatestAttemptRow> latest = repo.findUserLatestAttemptsGrouped(userId);

        boolean bySubject = "SUBJECT".equalsIgnoreCase(groupBy);
        if (bySubject) {
            // 같은 과목 내 여러 subclass 중 최신 시도만 남김
            Map<Long, UserLatestAttemptRow> latestBySubclass = latest.stream()
                    .collect(Collectors.toMap(
                            UserLatestAttemptRow::getSubclassId,
                            r -> r,
                            (a, b) -> a.getTestDate().isAfter(b.getTestDate()) ? a : b
                    ));
            latest = new ArrayList<>(latestBySubclass.values());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (UserLatestAttemptRow row : latest.stream()
                .sorted(Comparator.comparing(UserLatestAttemptRow::getTestDate))
                .toList()) {

            CohortCountRow c = repo.countCohortAndLess(row.getCsatDate(), row.getSubclassId(), row.getScore());
            int cohort = c.getCohort() == null ? 0 : c.getCohort().intValue();
            int less   = c.getLessCnt() == null ? 0 : c.getLessCnt().intValue();
            int percentile = cohort == 0 ? 0 : (int) Math.floor((less * 100.0) / cohort);
            int rank = Math.max(1, cohort - less);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("csatDate", bySubject ? null : row.getCsatDate());
            item.put("percentile", percentile);
            item.put("total", cohort);
            item.put("rank", rank);
            item.put("subclassId", bySubject ? row.getSubclassId() : null);
            item.put("score", row.getScore());
            out.add(item);
        }
        return out;
    }

    // ------- 묶음(선택+공통) 계산 -------
    private List<Long> resolveCohortSubclassIds(Long subclassId) {
        Long subjectId = repo.findSubjectIdBySubclass(subclassId);
        if (subjectId == null) return List.of(subclassId);

        List<Long> commons = repo.findCommonSubclassIds(subjectId); // optional=0
        List<Long> out = new ArrayList<>();
        out.add(subclassId);
        if (commons != null) out.addAll(commons);
        return out;
    }

    // ------- 스코프 해석: ALL / SUBJECT / SUBCLASS -------
    private List<Long> resolveScopeSubclassIds(String scope, Long subclassId, Long subjectId) {
        String s = scope == null ? "SUBCLASS" : scope.toUpperCase(Locale.ROOT);
        switch (s) {
            case "ALL":
                return repo.findAllSubclassIds();
            case "SUBJECT":
                if (subjectId == null)
                    throw new IllegalArgumentException("subjectId is required when scope=SUBJECT");
                return repo.findSubclassIdsBySubject(subjectId);
            case "SUBCLASS":
            default:
                if (subclassId == null)
                    throw new IllegalArgumentException("subclassId is required when scope=SUBCLASS");
                return resolveCohortSubclassIds(subclassId);
        }
    }

    // ------- 분포 -------
    @Override
    public Map<String, Object> getScoreDistribution(String csatDate, Long subclassId, Long subjectId,
                                                    String scope, int bin, Long userId) {
        List<Long> cohortIds = resolveScopeSubclassIds(scope, subclassId, subjectId);

        List<BinRow> rows = repo.buildHistogramIn(csatDate, cohortIds, bin);
        int cohort = 0;
        List<Map<String, Object>> bins = new ArrayList<>();
        for (BinRow r : rows) {
            int start = r.getBin() * bin;
            int end   = start + bin - 1;
            bins.add(Map.of("start", start, "end", end, "count", r.getCnt()));
            cohort += r.getCnt().intValue();
        }

        Integer myScore = null, myPercentile = null;
        boolean isSubclassScope = (scope == null) || "SUBCLASS".equalsIgnoreCase(scope);
        if (isSubclassScope && userId != null) {
            myScore = repo.findMyLatestScore(userId, csatDate, subclassId);
            if (myScore != null) {
                CohortCountRow c = repo.countCohortAndLessIn(csatDate, cohortIds, myScore);
                int cohortCnt = c.getCohort() == null ? 0 : c.getCohort().intValue();
                int less      = c.getLessCnt() == null ? 0 : c.getLessCnt().intValue();
                myPercentile  = cohortCnt == 0 ? 0 : (int) Math.floor((less * 100.0) / cohortCnt);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("csatDate", csatDate);
        if (subclassId != null) res.put("subclassId", subclassId);
        if (subjectId  != null) res.put("subjectId", subjectId);
        res.put("scope", isSubclassScope ? "SUBCLASS" : scope.toUpperCase(Locale.ROOT));
        res.put("binSize", bin);
        res.put("bins", bins);
        res.put("cohort", cohort);
        if (myScore != null)      res.put("myScore", myScore);
        if (myPercentile != null) res.put("myPercentile", myPercentile);
        return res;
    }

    // ------- KPI -------
    @Override
    public Map<String, Object> getExamKpis(String csatDate, Long subclassId, Long subjectId, String scope) {
        List<Long> cohortIds = resolveScopeSubclassIds(scope, subclassId, subjectId);

        int participants = repo.countParticipantsIn(csatDate, cohortIds);
        int attempts     = repo.countAttemptsIn(csatDate, cohortIds);
        List<Integer> scores = repo.findSortedScoresIn(csatDate, cohortIds);

        Double median = percentileOfSorted(scores, 50);
        Double p90    = percentileOfSorted(scores, 90);
        Double p10    = percentileOfSorted(scores, 10);
        Integer cut1  = (scope == null || "SUBCLASS".equalsIgnoreCase(scope))
                ? repo.findGrade1Cut(csatDate, subclassId)
                : null;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("csatDate", csatDate);
        if (subclassId != null) res.put("subclassId", subclassId);
        if (subjectId  != null) res.put("subjectId", subjectId);
        res.put("scope", scope == null ? "SUBCLASS" : scope.toUpperCase(Locale.ROOT));
        res.put("participants", participants);
        res.put("attempts", attempts);
        res.put("median", median);
        res.put("p90", p90);
        res.put("p10", p10);
        if (cut1 != null) res.put("grade1Cut", cut1);
        return res;
    }

    // ------- 회차 전체 재응시 -------
    @Override
    public Map<String, Object> getRetakeRate(String csatDate) {
        int participants = repo.countParticipantsAllSubjects(csatDate);
        int reattemptUsers = repo.countReattemptUsers(csatDate);
        Double avg = repo.avgAttemptsPerUser(csatDate);
        double rate = participants == 0 ? 0.0 : (reattemptUsers * 1.0 / participants);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("csatDate", csatDate);
        res.put("participants", participants);
        res.put("reattemptUsers", reattemptUsers);
        res.put("retakeRate", rate);
        res.put("avgAttemptsPerUser", avg == null ? 0.0 : avg);
        return res;
    }

    // ------- 유틸 -------
    private Double percentileOfSorted(List<Integer> sorted, int p) {
        if (sorted == null || sorted.isEmpty()) return null;
        if (p <= 0)   return (double) sorted.get(0);
        if (p >= 100) return (double) sorted.get(sorted.size() - 1);
        int n = sorted.size();
        int rank = (int) Math.ceil(p / 100.0 * n); // 1-based
        int idx = Math.max(0, Math.min(n - 1, rank - 1));
        return sorted.get(idx).doubleValue();
    }
}
