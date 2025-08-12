package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.repository.ReviewRepository;
import com.CBTServer.WebCSAT.repository.proj.AttemptRow;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewApiController {
    private final ReviewRepository reviewRepository;

    // 매핑 확인용
    @GetMapping("/ping")
    public String ping() { return "pong"; }

    @GetMapping("/attempts")
    public ResponseEntity<List<AttemptRow>> list(
            @RequestParam Long userId,
            @RequestParam(required = false) String csatDate,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long subclassId
    ) {

        List<AttemptRow> rows = reviewRepository.findAttempts(userId, csatDate, subjectId, subclassId);
        return ResponseEntity.ok(rows); // ✅ 빈 결과도 200 OK + []
    }
}

