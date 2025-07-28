package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.domain.SubclassCsatDate;
import com.CBTServer.WebCSAT.domain.Subject;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.repository.SubclassCsatDateRepository;
import com.CBTServer.WebCSAT.service.QuestionService;
import com.CBTServer.WebCSAT.service.SubclassCsatDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class QuestionApiController {
    private final QuestionService questionService;
    private final SubclassCsatDateRepository subclassCsatDateRepository;
    private final SubclassCsatDateService subclassCsatDateService;

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
    public ResponseEntity<?> addMockQuestion(@RequestBody Map<String, Object> body) {
        String csatDate = (String) body.get("csatDate");
        Long subjectId = ((Number) body.get("subjectId")).longValue();
        Long subclassId = ((Number) body.get("subclassId")).longValue();

        Optional<SubclassCsatDate> exists = subclassCsatDateRepository.findById_SubclassIdAndId_CsatDate(subclassId, csatDate);

        if (exists.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 해당 모의고사 문제가 존재합니다.");
        }

        // 등급컷 추출
        List<Integer> cuts = null;
        if (body.containsKey("cuts")) {
            cuts = ((List<?>) body.get("cuts")).stream()
                    .map(c -> ((Number) c).intValue())
                    .toList();
        }

        // 듣기 URL 매핑 추출
        Map<Long, String> subclassListeningUrlMap = new HashMap<>();
        if (body.containsKey("listeningUrlMap")) {
            Map<?, ?> rawMap = (Map<?, ?>) body.get("listeningUrlMap");
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Long key = Long.parseLong(entry.getKey().toString());
                String val = entry.getValue().toString();
                subclassListeningUrlMap.put(key, val);
            }
        }

        try {
            questionService.createMockQuestion(csatDate, subjectId, subclassId, cuts, subclassListeningUrlMap);
            return ResponseEntity.ok(Map.of("csatDate", csatDate, "subjectId", subjectId, "subclassId", subclassId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("듣기 과목에는 반드시 MP3를 지정해야 합니다.");
        }
    }

    @GetMapping("/api/csatDates")
    public ResponseEntity<List<String>> getAvailableDatesBySubclass(@RequestParam Long subclassId) {
        List<String> dates = questionService.getAvailableCsatDatesBySubclass(subclassId);
        return ResponseEntity.ok(dates);
    }

    @PutMapping("/api/cuts")
    public ResponseEntity<?> updateCuts(@RequestBody Map<String, Object> body) {
        Long subclassId = Long.parseLong(body.get("subclassId").toString());
        String csatDate = body.get("csatDate").toString();

        List<?> rawCuts = (List<?>) body.get("cuts");
        List<Integer> cuts = rawCuts.stream()
                .map(c -> Integer.parseInt(c.toString()))
                .toList();

        subclassCsatDateService.updateCuts(subclassId, csatDate, cuts);
        return ResponseEntity.ok().build();
    }
}
