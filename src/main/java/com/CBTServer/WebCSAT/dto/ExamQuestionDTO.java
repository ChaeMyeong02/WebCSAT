package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class ExamQuestionDTO {
    private Long joinId;
    private Long examId;
    private Long subclassId;
    private String selectedAnswer;
    private boolean isCorrect;
}
