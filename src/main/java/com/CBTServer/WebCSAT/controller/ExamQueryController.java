// src/main/java/com/CBTServer/WebCSAT/controller/ExamQueryController.java
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.repository.ExamReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) Long subclassId
    ) {
        return repo.findAllFiltered(
                (csatDate == null || csatDate.isBlank()) ? null : csatDate,
                subjectId,
                subclassId
        );
    }
}
