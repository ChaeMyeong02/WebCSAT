package com.CBTServer.WebCSAT.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExamReviewDTO {
    private Long examId;
    private String csatDate;
    private Long subclassId;
    private String subclassName;
    private LocalDateTime testDate;
    private Integer result;       // 점수

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long id;            // question_id
        private Integer number;     // num
        private String stem;        // question_title
        private List<String> options; // option1~option5
        private String correct;     // 'A'..'E'
        private String myAnswer;    // 'A'..'E'
        private String explanation; // question_context (원하면 다른 칼럼)
    }
}