package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExamDTO {
    private Long examId;
    private Long userId;
    private Long subclassId;
    private String csatDate;
    private int result;
    private LocalDateTime testDate;
    private Time duration;

    private List<Long> questionIds;
    private List<String> answers;

    public List<SubmittedAnswerDTO> toSubmittedAnswerList() {
        List<SubmittedAnswerDTO> result = new ArrayList<>();

        // 문제 개수는 questionIds 기준으로
        for (int i = 0; i < questionIds.size(); i++) {
            String ans = (i < answers.size()) ? answers.get(i) : null;
            result.add(new SubmittedAnswerDTO(questionIds.get(i), ans));
        }

        return result;
    }
}