package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.dto.AttemptTrendDTO;
import com.CBTServer.WebCSAT.repository.LookupRepository;
import com.CBTServer.WebCSAT.repository.StatsRepository;
import com.CBTServer.WebCSAT.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;
    private final LookupRepository repo; // repo.attemptTrend(uid) 를 제공하는 리포지토리
    private final StatsRepository statsRepository; // repo.attemptTrend(uid) 를 제공하는 리포지토리
    // 기존 그대로
    @GetMapping("/user/{userId}/percentile-trend")
    public ResponseEntity<List<Map<String, Object>>> getPercentileTrend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "CSAT") String groupBy
    ) {
        return ResponseEntity.ok(statsService.getPercentileTrend(userId, groupBy));
    }

    // ✅ scope/subjectId/subclassId 지원
    @GetMapping("/exam/score-distribution")
    public ResponseEntity<Map<String, Object>> getScoreDistribution(
            @RequestParam String csatDate,
            @RequestParam(required = false) Long subclassId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "SUBCLASS") String scope, // ALL|SUBJECT|SUBCLASS
            @RequestParam(defaultValue = "5") int bin,
            @RequestParam(required = false) Long userId
    ) {
        return ResponseEntity.ok(
                statsService.getScoreDistribution(csatDate, subclassId, subjectId, scope, bin, userId)
        );
    }

    // ✅ scope/subjectId/subclassId 지원
    @GetMapping("/exam/kpis")
    public ResponseEntity<Map<String, Object>> getExamKpis(
            @RequestParam String csatDate,
            @RequestParam(required = false) Long subclassId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "SUBCLASS") String scope
    ) {
        return ResponseEntity.ok(
                statsService.getExamKpis(csatDate, subclassId, subjectId, scope)
        );
    }

    @GetMapping("/admin/retake-rate")
    public ResponseEntity<Map<String, Object>> getRetakeRate(@RequestParam String csatDate) {
        return ResponseEntity.ok(statsService.getRetakeRate(csatDate));
    }

//
//    @GetMapping("/user/{uid}/attempt-trend")
//    public List<AttemptTrendDTO> attemptTrend(
//            @PathVariable Long uid,
//            @RequestParam(required = false) Long subjectId,
//            @RequestParam(required = false) Long subclassId,
//            @RequestParam(required = false) String csatDate
//    ) {
//        return repo.attemptTrend(uid, subjectId, subclassId, csatDate).stream()
//                .map(r -> new AttemptTrendDTO(
//                        ((Number) r[0]).longValue(),                 // examId
//                        ((Number) r[1]).longValue(),                 // subclassId
//                        (String) r[2],                               // csatDate
//                        ((java.sql.Timestamp) r[3]).toLocalDateTime(), // testDate
//                        ((Number) r[4]).intValue()                   // percentile
//                ))
//                .toList();
//    }

    @GetMapping("/user/{uid}/attempt-trend")
    public List<AttemptTrendDTO> attemptTrendUser(
            @PathVariable Long uid,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long subclassId,
            @RequestParam(required = false) String csatDate
    ){
        return repo.attemptTrendUser(uid, subjectId, subclassId, csatDate)
                .stream()
                .map(r -> new AttemptTrendDTO(
                        r[0] != null ? ((Number) r[0]).longValue() : null,     // examId
                        r[1] != null ? ((Number) r[1]).longValue() : null,     // subclassId
                        (String) r[2],                                         // csatDate
                        ((java.sql.Timestamp) r[3]).toLocalDateTime(),         // testDate
                        r[4] != null ? ((Number) r[4]).doubleValue() : null    // result (score)
                ))
                .toList();
    }

    @GetMapping("/admin/attempt-trend")
    public List<AttemptTrendDTO> attemptTrendAdmin(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long subclassId,
            @RequestParam(required = false) String csatDate
    ){
        return repo.attemptTrendAdmin(subjectId, subclassId, csatDate)
                .stream()
                .map(r -> new AttemptTrendDTO(
                        null,                                                  // examId (집계)
                        r[1] != null ? ((Number) r[1]).longValue() : null,     // subclassId
                        (String) r[2],                                         // csatDate
                        ((java.sql.Timestamp) r[3]).toLocalDateTime(),         // testDate (min)
                        r[4] != null ? ((Number) r[4]).doubleValue() : null    // result = 평균 점수
                ))
                .toList();
    }
}
