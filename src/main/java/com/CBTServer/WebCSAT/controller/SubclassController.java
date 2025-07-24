package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubclassController {

    private final SubclassRepository subclassRepository;

    @GetMapping("/api/takeExam/subclasses")
    public List<Subclass> getSubclasses(@RequestParam Long subjectId) {
        return subclassRepository.findAllBySubject_SubjectId(subjectId);
    }

}

