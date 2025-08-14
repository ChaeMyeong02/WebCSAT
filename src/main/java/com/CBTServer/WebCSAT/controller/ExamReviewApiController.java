// src/main/java/com/CBTServer/WebCSAT/controller/ExamReviewApiController.java
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.dto.ExamReviewDTO;
import com.CBTServer.WebCSAT.repository.ExamRepository;
import com.CBTServer.WebCSAT.repository.ReviewQueryRepository;
import com.CBTServer.WebCSAT.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamReviewApiController {

    private final ExamRepository examRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/{examId}/review")
    public ExamReviewDTO getReview(@PathVariable Long examId) {
        Long me = currentUserService.requiredUserId();

        // 내 시험인지 확인 (없으면 404)
        examRepository.findByExamIdAndUser_UserId(examId, me)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 메타 + 문항
        ExamReviewDTO dto = reviewQueryRepository.loadMeta(examId, me);
        dto.setItems(reviewQueryRepository.loadItems(examId, me));
        return dto;
    }
}
