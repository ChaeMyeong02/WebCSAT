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
public class GradeTrendDTO {
    private String subclassName;
    private List<Integer> scores; // 같은 모의고사 내 점수 추이
    private List<String> grades;
}
