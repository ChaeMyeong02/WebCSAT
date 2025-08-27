// src/main/java/com/CBTServer/WebCSAT/controller/ExamQueryController.java
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.repository.ExamReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamQueryController {

    private final ExamReadRepository repo;

    @GetMapping
    public List<ExamReadRepository.ExamRow> list(
            @RequestParam(required = false) String csatDate,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long subclassId,
            @RequestParam(required = false) Long userId,

            // 예: from=2025-07-01&to=2025-08-01
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDateTime fromTs = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toTs   = (to   != null) ? to.atStartOfDay()   : null;

        return repo.findAllFilteredRange(
                (csatDate == null || csatDate.isBlank()) ? null : csatDate,
                subjectId,
                subclassId,
                userId,
                fromTs,
                toTs
        );
    }
}
