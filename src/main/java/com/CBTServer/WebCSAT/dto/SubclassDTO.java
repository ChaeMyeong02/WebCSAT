package com.CBTServer.WebCSAT.dto;

import com.CBTServer.WebCSAT.domain.Subclass;
import lombok.*;

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
}
