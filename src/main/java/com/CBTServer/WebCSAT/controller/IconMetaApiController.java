package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.IconMeta;
import com.CBTServer.WebCSAT.domain.ImageMeta;
import com.CBTServer.WebCSAT.dto.IconMetaDTO;
import com.CBTServer.WebCSAT.dto.ImageMetaDTO;
import com.CBTServer.WebCSAT.repository.IconMetaRepository;
import com.CBTServer.WebCSAT.repository.ImageMetaRepository;
import com.CBTServer.WebCSAT.service.IconService;
import com.CBTServer.WebCSAT.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IconMetaApiController {

    private final IconMetaRepository iconMetaRepository;
    private final IconService iconService;

    @GetMapping("/api/icons")
    public ResponseEntity<List<IconMetaDTO>> getIcons() {
        List<IconMeta> icons = iconMetaRepository.findAll();

        List<IconMetaDTO> result = icons.stream()
                .map(IconMetaDTO::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/icons")
    public ResponseEntity<String> uploadIcon(
            @RequestParam MultipartFile file
    ) throws IOException {
        String url = iconService.uploadAndRegisterIfNotExists(file);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/api/icons")
    public ResponseEntity<Void> deleteIcon(@RequestParam("url") String url) {
        iconService.deleteIcon(url);
        return ResponseEntity.ok().build();
    }

}

