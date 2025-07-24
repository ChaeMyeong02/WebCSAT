package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            QuestionDTO dto = questionService.newQuestion(questionDTO.getCsatDate(), questionDTO.getSubjectId(), questionDTO.getSubclassId(), questionDTO.getNum());
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @PutMapping("/api/question/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable("questionId") Long questionId, @RequestBody QuestionDTO questionDTO) {
        try {
            System.out.println("title: " + questionDTO.getQuestionTitle());
            System.out.println("option1: " + questionDTO.getOption1());
            System.out.println("article: " + questionDTO.getQuestionArticle());
            QuestionDTO dto = questionService.updateQuestion(questionId, questionDTO);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @GetMapping("/api/csatDates")
    public ResponseEntity<List<String>> getAvailableDatesBySubclass(@RequestParam Long subclassId) {
        List<String> dates = questionService.getAvailableCsatDatesBySubclass(subclassId);
        return ResponseEntity.ok(dates);
    }
}