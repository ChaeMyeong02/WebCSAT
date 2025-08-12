package com.CBTServer.WebCSAT.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewAttemptDTO {
    private Long examId;                 // 응시 시도 PK
    private String csatDate;             // 회차 (예: 2024년11월)
    private Long subclassId;             // 세부과목 ID
    private String subclassName;         // 세부과목명 (상단 표시용)
    private List<ReviewQuestionDTO> questions; // 문항+선지+정오표시
}