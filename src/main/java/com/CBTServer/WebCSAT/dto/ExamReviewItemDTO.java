package com.CBTServer.WebCSAT.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
@Data
@NoArgsConstructor @AllArgsConstructor
public class ExamReviewItemDTO {
    private Long id;
    private Integer number;
    private String stem;
    // 보기 5개 고정 (프로젝트에 따라 options 배열을 써도 됨)
    private String opt1;
    private String opt2;
    private String opt3;
    private String opt4;
    private String opt5;

    private Object correct;   // 1~5 / 0~4 / 'A'~'E' 모두 허용 (프런트에서 보정)
    private Object myAnswer;  // 동일
    private String explanation;
}
