package com.CBTServer.WebCSAT.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewAttemptSummaryDTO {
    private Long examId;
    private String csatDate;
    private Long subclassId;
    private String subclassName;
    private String testDate;   // ISO 문자열 or yyyy-MM-dd HH:mm
    private Integer score;
}