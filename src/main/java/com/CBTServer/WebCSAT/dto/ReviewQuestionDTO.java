package com.CBTServer.WebCSAT.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewQuestionDTO {
    private Long questionId;         // 문항 PK
    private int number;              // 문항 번호(1,2,3..)
    private String content;          // 문항 본문(문제)
    private List<ReviewChoiceDTO> choices; // 선지 목록(라벨 순 정렬)

    private Long selectedChoiceId;   // 내가 고른 선지
    private Long correctChoiceId;    // 정답 선지
    private boolean correct;         // 정답 여부 (selected == correct)
}