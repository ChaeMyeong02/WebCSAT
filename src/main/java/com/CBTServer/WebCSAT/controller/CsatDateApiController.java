package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.dto.CsatDateDTO;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import com.CBTServer.WebCSAT.service.CsatDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
public class CsatDateApiController {
    private final CsatDateRepository csatDateRepository;

    @PostMapping("/api/csat-date")
    public ResponseEntity<?> addCsatDate(@RequestBody Map<String, String> payload) {
        String csatDate = payload.get("csatDate");
        if (csatDate == null || csatDate.isBlank()) return ResponseEntity.badRequest().build();

        if (csatDateRepository.existsById(csatDate)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 중복
        }

        csatDateRepository.save(new CsatDate(csatDate)); // csatDate가 PK인 엔티티
        return ResponseEntity.ok().build();
    }
}
