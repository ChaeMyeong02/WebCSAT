// src/main/java/com/CBTServer/WebCSAT/controller/AdminExamReviewApiController.java
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.dto.ExamReviewDTO;
import com.CBTServer.WebCSAT.repository.ReviewQueryRepository;
import com.CBTServer.WebCSAT.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// (예시) src/main/java/.../AdminExamReviewApiController.java
@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExamReviewApiController {

    private final ReviewQueryRepository reviewQueryRepository;
    private final ExamRepository examRepository;

    @GetMapping("/{examId}/review")
    public ExamReviewDTO adminReview(@PathVariable Long examId) {
        // 존재 확인만
        examRepository.findByExamId(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ExamReviewDTO dto = reviewQueryRepository.loadMetaByExam(examId);
        dto.setItems(reviewQueryRepository.loadItemsForExam(examId)); // ✅ userId 제한 없음
        return dto;
    }
}
