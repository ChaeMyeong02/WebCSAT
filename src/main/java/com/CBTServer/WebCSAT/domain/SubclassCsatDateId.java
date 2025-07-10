package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SubclassCsatDateId implements Serializable {
    private Long subclassId;
    private String csatDate;
}
