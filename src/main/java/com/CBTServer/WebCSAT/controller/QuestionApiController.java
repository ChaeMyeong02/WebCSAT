package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.domain.Subject;
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

    @DeleteMapping("/api/question/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        try {
            questionService.remove(questionId);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }


    @PutMapping("/api/question/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable("questionId") Long questionId, @RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO dto = questionService.updateQuestion(questionId, questionDTO);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @PostMapping("/api/question/bank")
    public ResponseEntity<?> addBankQuestion(@RequestParam("subjectId") Long subjectId, @RequestParam("subclassId") Long subclassId) {
        QuestionDTO questionDTO = questionService.createBankQuestion("문제은행용", subjectId, subclassId, 0);
        return ResponseEntity.ok(Map.of("questionId", questionDTO.getQuestionId()));
    }

    @PostMapping("/api/question/mock")
    public ResponseEntity<?> addMockQuestion(@RequestParam("csatDate") String csatDate, @RequestParam("subjectId") Long subjectId, @RequestParam("subclassId") Long subclassId) {
        questionService.createMockQuestion(csatDate, subjectId, subclassId);
        return ResponseEntity.ok(Map.of("csatDate", csatDate, "subjectId", subjectId, "subclassId", subclassId));
    }
}
