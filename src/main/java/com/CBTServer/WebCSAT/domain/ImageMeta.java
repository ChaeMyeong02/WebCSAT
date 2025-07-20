package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_meta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImageMeta {
    @Id
    @Column(length = 512)
    private String url;  // S3 URL (PK)

    @Column(name = "s3_key", nullable = false, length = 255)
    private String s3Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "csat_date", referencedColumnName = "csat_date")
    private CsatDate csatDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id", referencedColumnName = "subclass_id")
    private Subclass subclass;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}