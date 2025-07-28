package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.SubclassCsatDate;
import com.CBTServer.WebCSAT.domain.SubclassCsatDateId;
import com.CBTServer.WebCSAT.repository.SubclassCsatDateRepository;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class ListeningService {

    private final AmazonS3Client amazonS3Client;
    private final SubclassCsatDateRepository subclassCsatDateRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = "listening/" + UUID.randomUUID() + extension;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3Client.putObject(bucket, fileName, file.getInputStream(), metadata);

            return amazonS3Client.getUrl(bucket, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    public void replaceListeningUrl(Long subclassId, String csatDate, String newUrl) {
        SubclassCsatDateId id = new SubclassCsatDateId(subclassId, csatDate);
        SubclassCsatDate entity = subclassCsatDateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 시험 정보가 존재하지 않습니다."));

        String oldUrl = entity.getListeningUrl();

        // 기존 S3 파일 삭제
        if (oldUrl != null && oldUrl.contains(bucket)) {
            String key = extractS3KeyFromUrl(oldUrl);
            amazonS3Client.deleteObject(bucket, key);
        }

        // 새 URL 설정 및 저장
        entity.setListeningUrl(newUrl);
        subclassCsatDateRepository.save(entity);
    }


    private String extractS3KeyFromUrl(String url) {
        // ex: https://bucket-name.s3.region.amazonaws.com/listening/uuid.mp3
        int idx = url.indexOf(".amazonaws.com/");
        if (idx == -1) return url; // fallback
        return url.substring(idx + ".amazonaws.com/".length());
    }
}
