package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.service.ListeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ListeningApiController {
    private final ListeningService listeningService;
    @PostMapping("/api/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = listeningService.upload(file);
        return ResponseEntity.ok(fileUrl);
    }
}
