package com.CBTServer.WebCSAT.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewChoiceDTO {
    private Long choiceId;   // 선지 PK
    private String label;    // A/B/C/D ...
    private String content;  // 선지 내용
}