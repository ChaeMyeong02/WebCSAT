package com.CBTServer.WebCSAT.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SubjectDTO {
    private Long subjectId;
    private String subjectName;
    private int option;
    private int min;
}
