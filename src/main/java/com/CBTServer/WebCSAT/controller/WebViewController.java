package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubclassDTO;
import com.CBTServer.WebCSAT.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class WebViewController {
    private final QuestionService questionService;

    // Index
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth(Authentication authentication) {
        System.out.println("Principal: " + authentication.getPrincipal());
        System.out.println("Authorities: " + authentication.getAuthorities());
        return ResponseEntity.ok(authentication.getAuthorities());
    }

}
