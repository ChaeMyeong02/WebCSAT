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

    // ✅ 캐시: key → CacheEntry
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // ✅ 캐시 만료 시간 (예: 1일)
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    // ✅ 캐시 최대 개수 (LRU 유지)
    private static final int MAX_CACHE_SIZE = 100;

    public Mono<String> generateAdviceFromExams(List<ExamDTO> exams) {
        if (exams == null || exams.isEmpty()) {
            return Mono.just("최근 시험 데이터가 부족하여 분석할 수 없습니다.");
        }

        // ✅ 안정적인 캐시 키 생성 (시험 날짜 + 점수 기반)
        String key = buildCacheKey(exams);

        // ✅ 오래된 항목 정리
        cleanupCache();

        // ✅ 캐시 조회
        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            cached.touch(); // LRU 최신화
            System.out.println("💾 캐시 HIT: " + key);
            return Mono.just(cached.response());
        }

        // ✅ 캐시 MISS → OpenAI 호출
        System.out.println("🧠 캐시 MISS → OpenAI 호출: " + key);
        String prompt = buildPrompt(exams);

        return openAiClient.generateTextAsync(prompt)
                .doOnNext(advice -> putCache(key, advice));
    }

    /** 안정적인 캐시 키 생성 (시험일 + 점수 기반) */
    private String buildCacheKey(List<ExamDTO> exams) {
        StringBuilder sb = new StringBuilder();
        exams.forEach(e -> {
            String date = e.getTestDate() != null ? e.getTestDate().toString() : "null";
            sb.append(date).append(":").append(e.getResult()).append("|");
        });
        return sb.toString();
    }

    /** 프롬프트 구성 */
    private String buildPrompt(List<ExamDTO> exams) {
        StringBuilder sb = new StringBuilder("다음은 학생의 최근 시험 결과입니다:\n\n");

        for (ExamDTO e : exams) {
            String dateStr = (e.getTestDate() != null) ? e.getTestDate().toString() : "(날짜 없음)";
            sb.append(String.format("- %s: %d점\n", dateStr, e.getResult()));
        }

        double avg = exams.stream()
                .mapToInt(ExamDTO::getResult)
                .average()
                .orElse(0);

        sb.append(String.format("\n최근 평균 점수는 %.1f점입니다.\n", avg));
        sb.append("이 데이터를 기반으로 학생의 학습 추세를 분석하고, 구체적인 학습 방향 조언을 3~5줄 정도로 작성해주세요.");

        return sb.toString();
    }

    /** 캐시에 항목 추가 (LRU 관리 포함) */
    private void putCache(String key, String response) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            removeOldestEntry();
        }
        cache.put(key, new CacheEntry(response, Instant.now()));
        System.out.println("🗃️ 캐시에 새 응답 저장: " + key);
    }

    /** 캐시 정리 (만료된 항목 제거) */
    private void cleanupCache() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /** 가장 오래된 캐시 항목 제거 (LRU 스타일) */
    private void removeOldestEntry() {
        Optional<Map.Entry<String, CacheEntry>> oldest = cache.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().lastAccessed()));
        oldest.ifPresent(e -> {
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

        String response() {
            return response;
        }

        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(CACHE_TTL));
        }

        Instant lastAccessed() {
            return lastAccessed;
        }

        void touch() {
            this.lastAccessed = Instant.now();
        }
    }
}
