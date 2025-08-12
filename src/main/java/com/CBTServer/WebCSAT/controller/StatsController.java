package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

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
}
