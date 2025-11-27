package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.dto.ExamDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AiAdviceService {

    private final OpenAiClient openAiClient;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * AI 조언 생성
     * @param exams 시험 데이터 목록
     * @param role  사용자 역할 (USER / ADMIN)
     * @param mode  분석 모드 (mine / one / all)
     */
    public Mono<String> generateAdviceFromExams(List<ExamDTO> exams, String role, String mode) {
        if (exams == null || exams.isEmpty()) {
            return Mono.just("최근 시험 데이터가 부족하여 분석할 수 없습니다.");
        }

        String key = buildCacheKey(exams, role, mode);
        cleanupCache();

        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            cached.touch();
            System.out.println("💾 캐시 HIT: " + key);
            return Mono.just(cached.response());
        }

        System.out.println("🧠 캐시 MISS → OpenAI 호출: " + key);
        String prompt = buildPrompt(exams, role, mode);

        return openAiClient.generateTextAsync(prompt)
                .doOnNext(advice -> putCache(key, advice));
    }

    /** 캐시 키: 시험데이터 + 역할 + 모드 */
    private String buildCacheKey(List<ExamDTO> exams, String role, String mode) {
        StringBuilder sb = new StringBuilder(role + "|" + mode + "|");
        exams.forEach(e -> {
            String date = e.getTestDate() != null ? e.getTestDate().toString() : "null";
            sb.append(date).append(":").append(e.getResult()).append("|");
        });
        return sb.toString();
    }

    /** 역할/모드별 프롬프트 */
    private String buildPrompt(List<ExamDTO> exams, String role, String mode) {
        double avg = exams.stream().mapToInt(ExamDTO::getResult).average().orElse(0);

        StringBuilder sb = new StringBuilder();

        // 🧠 역할/모드별 프롬프트
        if ("ADMIN".equalsIgnoreCase(role) && "all".equalsIgnoreCase(mode)) {
            sb.append("""
        당신은 교육 통계 분석가입니다.
        아래 데이터는 한 학생의 성적이 아니라, 전체 학생들의 시험별 평균 점수 데이터입니다.
        각 줄은 시험 날짜와 해당 시험의 평균 점수를 의미합니다.
        학생 개인이 아닌 전체 집단의 성취 경향과 학습 상태를 분석하세요.
        """);
            sb.append("\n\n📊 전체 학생 평균 점수 데이터:\n");
        }
        else if ("one".equalsIgnoreCase(mode)) {
                sb.append("""
                당신은 교사이자 학습 분석 전문가입니다.
                아래는 특정 학생의 성적 기록입니다.
                이 학생의 점수를 전체 평균과 비교하여 상대적 강약점을 분석하세요.
                교사가 개별 지도를 위해 참고할 수 있는 형태로 제시하세요.
                """);
                sb.append("\n\n📄 특정 학생 점수 데이터:\n");

            }

         else if("mine".equalsIgnoreCase(mode)){
            sb.append("""
            당신은 수험생의 AI 멘토입니다.
            아래는 학생 본인의 시험 결과입니다.
            강점과 약점을 분석하고, 다음 시험을 위한 공부 전략을 제시하세요.
            """);
            sb.append("\n\n📄 학생 시험 결과:\n");
        }

        // ✅ 시험 데이터 나열
        for (ExamDTO e : exams) {
            String dateStr = (e.getTestDate() != null) ? e.getTestDate().toString() : "(날짜 없음)";
            sb.append(String.format("- %s: %d점\n", dateStr, e.getResult()));
        }

        sb.append(String.format("\n평균 점수는 %.1f점입니다.\n", avg));

        // ✅ 모드별 마무리 문장
        if ("ADMIN".equalsIgnoreCase(role) && "all".equalsIgnoreCase(mode)) {
            sb.append("\n이 데이터를 바탕으로 전체 학급의 학습 방향을 제안하는 4~6줄 정도의 요약 분석을 작성하세요.");
        } else if ("ADMIN".equalsIgnoreCase(role) && "one".equalsIgnoreCase(mode)) {
            sb.append("\n이 학생의 학습 성향과 개선 방향을 중심으로 3~5줄의 분석 결과를 작성하세요.");
        } else {
            sb.append("\n이 데이터를 기반으로 학생의 학습 추세를 분석하고, 구체적인 공부 조언을 3~5줄 정도로 작성하세요.");
        }

        return sb.toString();
    }


    /** 캐시에 항목 추가 */
    private void putCache(String key, String response) {
        if (cache.size() >= MAX_CACHE_SIZE) removeOldestEntry();
        cache.put(key, new CacheEntry(response, Instant.now()));
        System.out.println("🗃️ 캐시에 새 응답 저장: " + key);
    }

    /** 만료 캐시 정리 */
    private void cleanupCache() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /** 오래된 캐시 제거 */
    private void removeOldestEntry() {
        cache.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().lastAccessed()))
                .ifPresent(e -> {
                    cache.remove(e.getKey());
                    System.out.println("🧹 오래된 캐시 제거: " + e.getKey());
                });
    }

    /** 캐시 데이터 구조 */
    private static class CacheEntry {
        private final String response;
        private final Instant createdAt;
        private Instant lastAccessed;

        CacheEntry(String response, Instant createdAt) {
            this.response = response;
            this.createdAt = createdAt;
            this.lastAccessed = createdAt;
        }

        String response() { return response; }
        boolean isExpired() { return Instant.now().isAfter(createdAt.plus(CACHE_TTL)); }
        Instant lastAccessed() { return lastAccessed; }
        void touch() { this.lastAccessed = Instant.now(); }
    }
}
