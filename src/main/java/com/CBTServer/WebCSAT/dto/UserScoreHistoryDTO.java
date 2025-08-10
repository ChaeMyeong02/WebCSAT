package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserScoreHistoryDTO {
    private long examId;
    private String csatDate;       // 시험 날짜
    private String subclassName;   // 세부 과목명
    private int score;             // 점수
    private String grade;          // "1등급" 등
}
