package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
public class QuestionApiController {
    private final QuestionService questionService;

    @GetMapping("/api/question/{questionId}")
    public ResponseEntity<?> getQuestion(@PathVariable Long questionId) {
        try {
            QuestionDTO dto = questionService.getQuestion(questionId);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @PostMapping("/api/question")
    public ResponseEntity<?> getQuestion(@RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO dto = questionService.newQuestion(questionDTO.getCsatDate(), questionDTO.getSubjectId(), questionDTO.getSubclassId(), questionDTO.getNum(), questionDTO.isQuestionType());
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @PutMapping("/api/question/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable("id") Long questionId, @RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO dto = questionService.updateQuestion(questionId, questionDTO);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }
}
