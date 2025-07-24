package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubclassDTO;
import com.CBTServer.WebCSAT.dto.SubjectDTO;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import com.CBTServer.WebCSAT.service.SubjectSubclassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class SubjectSubclassApiController {
    private final SubjectSubclassService subjectSubclassService;
    private final SubclassRepository subclassRepository;


    @GetMapping("/api/subject")
    public List<SubjectDTO> getAllSubject() {
        return subjectSubclassService.findAllSubject();
    }

    @GetMapping("/api/subclasses")
    public List<SubclassDTO> getSubclasses(@RequestParam Long subjectId) {
        List<Subclass> subclassList = subclassRepository.findAllBySubject_SubjectId(subjectId);
        return subclassList.stream()
                .map(SubclassDTO::from)
                .collect(Collectors.toList());
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

    @PutMapping("/api/subject/{subjectId}")
    public ResponseEntity<?> updateSubject(@PathVariable("subjectId") Long subjectId, @RequestBody SubjectDTO subjectDTO) {
        try {
            SubjectDTO updated = subjectSubclassService.updateSubject(subjectId, subjectDTO);
            return ResponseEntity.ok().body(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "과목이 존재하지 않습니다."));
        }
    }

    @PutMapping("/api/subclass/{subclassId}")
    public ResponseEntity<?> updateSubclass(@PathVariable("subclassId") Long subclassId, @RequestBody SubclassDTO subclassDTO) {
        try {
            SubclassDTO updated = subjectSubclassService.updateSubclass(subclassId, subclassDTO);
            return ResponseEntity.ok().body(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "선택과목이 존재하지 않습니다."));
        }
    }

    @DeleteMapping("/api/subject/{subjectId}")
    public ResponseEntity<?> deleteSubject(@PathVariable("subjectId") Long subjectId) {
        try {
            subjectSubclassService.deleteSubject(subjectId);
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "과목이 존재하지 않습니다."));
        }
    }

    @DeleteMapping("/api/subclass/{subclassId}")
    public ResponseEntity<?> deleteSubclass(@PathVariable("subclassId") Long subclassId) {
        try {
            subjectSubclassService.deleteSubclass(subclassId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "선택과목이 존재하지 않습니다."));
        }
    }
}
