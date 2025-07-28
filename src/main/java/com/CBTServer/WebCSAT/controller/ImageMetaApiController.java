package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.ImageMeta;
import com.CBTServer.WebCSAT.dto.ImageMetaDTO;
import com.CBTServer.WebCSAT.repository.ImageMetaRepository;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ImageMetaApiController {

    private final ImageMetaRepository imageMetaRepository;
    private final ImageService imageService;

    @GetMapping("/api/images")
    public ResponseEntity<List<ImageMetaDTO>> getImagesByCsatDateAndSubclass(
            @RequestParam(required = false) String csatDate,
            @RequestParam(required = false) Long subclassId
    ) {
        List<ImageMeta> images;

        if (csatDate != null && subclassId != null) {
            images = imageMetaRepository.findByCsatDate_CsatDateAndSubclass_SubclassId(csatDate, subclassId);
        } else if (csatDate != null) {
            images = imageMetaRepository.findByCsatDate_CsatDate(csatDate);
        } else if (subclassId != null) {
            images = imageMetaRepository.findBySubclass_SubclassId(subclassId);
        } else {
            images = imageMetaRepository.findAll(); // 전체 이미지 반환
        }

        List<ImageMetaDTO> result = images.stream()
                .map(ImageMetaDTO::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/images")
    public ResponseEntity<String> uploadImage(
            @RequestParam MultipartFile file,
            @RequestParam String csatDate,
            @RequestParam Long subclassId
    ) throws IOException {
        String url = imageService.uploadAndRegisterIfNotExists(file, csatDate, subclassId);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/api/images")
    public ResponseEntity<?> deleteImage(@RequestParam("url") String url) {
        try {
            imageService.deleteImage(url);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT) // 409 Conflict
                    .body(e.getMessage()); // 메시지를 그대로 보냄
        }
    }

}

