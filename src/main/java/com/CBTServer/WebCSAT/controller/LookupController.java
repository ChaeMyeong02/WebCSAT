package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.repository.LookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lookup")
@RequiredArgsConstructor
public class LookupController {
    private final LookupRepository repo;

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects() {
        return repo.findSubjects().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getName());
                    return m;
                })
                .collect(Collectors.toList());  // JDK8+ 호환
    }

    @GetMapping("/subclasses")
    public List<Map<String, Object>> subclasses(@RequestParam Long subjectId) {
        return repo.findSubclasses(subjectId).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getName());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/csat-dates")
    public List<String> csatDates(@RequestParam Long subclassId) {
        return repo.findCsatDatesForSubclass(subclassId);
    }

    // ✅ 새로 추가: 전체 과목 기준 회차 목록
    @GetMapping("/csat-dates-global")
    public List<String> csatDatesGlobal() {
        return repo.findAllCsatDates();
    }
}
