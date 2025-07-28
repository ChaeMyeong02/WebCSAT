package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.SubclassCsatDate;
import com.CBTServer.WebCSAT.domain.SubclassCsatDateId;
import com.CBTServer.WebCSAT.repository.SubclassCsatDateRepository;
import com.CBTServer.WebCSAT.service.ListeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class ListeningApiController {
    private final ListeningService listeningService;
    private final SubclassCsatDateRepository subclassCsatDateRepository;

    @PostMapping("/api/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = listeningService.upload(file);
        return ResponseEntity.ok(fileUrl);
    }

    @Transactional
    @PostMapping("/api/listening/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        String url = listeningService.upload(file); // 기존 S3 업로드 코드
        return ResponseEntity.ok(url);
    }

    @Transactional
    @PutMapping("/api/listening/update")
    public ResponseEntity<?> updateListeningUrl(@RequestBody Map<String, Object> body) {
        Long subclassId = Long.parseLong(body.get("subclassId").toString());
        String csatDate = body.get("csatDate").toString();
        String newUrl = body.get("url").toString();

        listeningService.replaceListeningUrl(subclassId, csatDate, newUrl);

        return ResponseEntity.ok().build();
    }


}
