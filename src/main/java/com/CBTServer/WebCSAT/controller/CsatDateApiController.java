package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.dto.CsatDateDTO;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.service.CsatDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
public class CsatDateApiController {
    private final CsatDateService csatDateService;

    @PostMapping("/api/castDate/{dateStr}")
    public ResponseEntity<?> addCastDate(@PathVariable("dateStr") String castDate) {
        try {
            CsatDateDTO dto = csatDateService.saveIfNotExist(castDate);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "이미 존재하는 날짜입니다."));
        }
    }
}
