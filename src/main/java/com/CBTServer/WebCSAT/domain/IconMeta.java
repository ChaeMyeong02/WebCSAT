package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "icon_meta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IconMeta {
    @Id
    @Column(length = 512)
    private String url;  // S3 URL (PK)

    @Column(name = "s3_key", nullable = false, length = 255)
    private String s3Key;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}