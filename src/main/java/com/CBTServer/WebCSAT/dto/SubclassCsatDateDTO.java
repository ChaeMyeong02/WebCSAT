package com.CBTServer.WebCSAT.dto;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.domain.SubclassCsatDateId;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubclassCsatDateDTO {
    private Long subclassId;
    private String csatDate;
}
