package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.ImageMeta;
import com.CBTServer.WebCSAT.dto.ImageMetaDTO;
import com.CBTServer.WebCSAT.repository.ImageMetaRepository;
import com.CBTServer.WebCSAT.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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
}

