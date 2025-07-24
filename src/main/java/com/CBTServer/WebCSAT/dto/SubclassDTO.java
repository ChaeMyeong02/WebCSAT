package com.CBTServer.WebCSAT.dto;

import com.CBTServer.WebCSAT.domain.Subclass;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SubclassDTO {
    private Long subclassId;
    private String subclassName;
    private int count;
    private boolean listening;
    private boolean optional;
    private Long subjectId;

    private List<QuestionDTO> questions;

    public static SubclassDTO from(Subclass subclass) {
        return SubclassDTO.builder()
                .subclassId(subclass.getSubclassId())
                .subclassName(subclass.getSubclassName())
                .count(subclass.getCount())
                .optional(subclass.isOptional())
                .subjectId(subclass.getSubject() != null ? subclass.getSubject().getSubjectId() : null)
                .questions(subclass.getQuestions() != null ?
                        subclass.getQuestions().stream()
                                .map(QuestionDTO::new)
                                .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

}
