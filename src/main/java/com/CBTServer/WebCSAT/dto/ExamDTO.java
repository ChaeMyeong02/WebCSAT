package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalDateTime;

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
}
