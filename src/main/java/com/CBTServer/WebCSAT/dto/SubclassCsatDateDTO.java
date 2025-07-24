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
    private String listeningUrl;
    private Integer cut1;
    private Integer cut2;
    private Integer cut3;
    private Integer cut4;
    private Integer cut5;
    private Integer cut6;
    private Integer cut7;
    private Integer cut8;
    private Integer cut9;
}
