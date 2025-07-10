package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubclassDTO;
import com.CBTServer.WebCSAT.dto.SubjectDTO;
import com.CBTServer.WebCSAT.service.SubjectSubclassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class SubjectSubclassApiController {
    private final SubjectSubclassService subjectSubclassService;

    @GetMapping("/api/subject")
    public List<SubjectDTO> getAllSubject() {
        return subjectSubclassService.findAllSubject();
    }

    @GetMapping("/api/subclass")
    public List<SubclassDTO> getAllSubclass() {
        return subjectSubclassService.findAllSubclass();
    }

    @GetMapping("/api/subject/{subjectId}")
    public ResponseEntity<?> getSubject(@PathVariable("subjectId") Long subjectId) {
        try {
            SubjectDTO dto = subjectSubclassService.findBySubjectId(subjectId);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @GetMapping("/api/subclass/{subclassId}")
    public ResponseEntity<?> getSubclass(@PathVariable("subclassId") Long subclassId) {
        try {
            SubclassDTO dto = subjectSubclassService.findBySubclassId(subclassId);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 문제 번호입니다."));
        }
    }

    @PostMapping("/api/subject")
    public ResponseEntity<?> postSubject(@RequestBody SubjectDTO subjectDTO) {
        try {
            SubjectDTO dto = subjectSubclassService.saveSubject(subjectDTO);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "오류가 발생하였습니다."));
        }
    }

    @PostMapping("/api/subclass")
    public ResponseEntity<?> postSubclass(@RequestBody SubclassDTO subclassDTO) {
        try {
            SubclassDTO dto = subjectSubclassService.saveSubclass(subclassDTO);
            return ResponseEntity.ok().body(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "오류가 발생하였습니다."));
        }
    }
}
