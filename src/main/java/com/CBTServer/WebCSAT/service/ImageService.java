package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.ImageMeta;
import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import com.CBTServer.WebCSAT.repository.ImageMetaRepository;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
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
public class ImageService {
    private final AmazonS3 amazonS3;
    private final ImageMetaRepository imageMetaRepository;
    private final CsatDateRepository csatDateRepository;
    private final SubclassRepository subclassRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadAndRegisterIfNotExists(MultipartFile file, String csatDateStr, Long subclassId) throws IOException {
        // 파일명 안전 처리
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String sanitizedFilename = originalFilename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        String s3Key = String.format("%s/%s/%s_%s", csatDateStr, subclassId, UUID.randomUUID(), sanitizedFilename);

        // S3 업로드
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, file.getInputStream(), metadata));

        String url = amazonS3.getUrl(bucketName, s3Key).toString();

        CsatDate csatDate = csatDateRepository.findById(csatDateStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid csatDate: " + csatDateStr));

        Subclass subclass = subclassRepository.findById(subclassId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subclassId: " + subclassId));

        // DB 저장
        ImageMeta meta = ImageMeta.builder()
                .s3Key(s3Key)
                .url(url)
                .csatDate(csatDate)
                .subclass(subclass)
                .createdAt(LocalDateTime.now())
                .build();

        imageMetaRepository.save(meta);
        return url;
    }
}
