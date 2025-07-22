package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.IconMeta;
import com.CBTServer.WebCSAT.domain.ImageMeta;
import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import com.CBTServer.WebCSAT.repository.IconMetaRepository;
import com.CBTServer.WebCSAT.repository.ImageMetaRepository;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IconService {
    private final AmazonS3 amazonS3;
    private final IconMetaRepository iconMetaRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadAndRegisterIfNotExists(MultipartFile file) throws IOException {
        // 파일명 안전 처리
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String sanitizedFilename = originalFilename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        String s3Key = String.format("%s/%s_%s", "icon", UUID.randomUUID(), sanitizedFilename);

        // S3 업로드
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, file.getInputStream(), metadata));

        String url = amazonS3.getUrl(bucketName, s3Key).toString();


        // DB 저장
        IconMeta meta = IconMeta.builder()
                .s3Key(s3Key)
                .url(url)
                .createdAt(LocalDateTime.now())
                .build();

        iconMetaRepository.save(meta);
        return url;
    }
    public void deleteIcon(String url) {
        iconMetaRepository.deleteById(url);
    }
}
