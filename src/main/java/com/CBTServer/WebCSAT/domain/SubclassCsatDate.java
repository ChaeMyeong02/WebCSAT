package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subclass_csat_date")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubclassCsatDate {
    @EmbeddedId
    private SubclassCsatDateId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subclassId")
    @JoinColumn(name = "subclass_id")
    private Subclass subclass;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("csatDate")
    @JoinColumn(name = "csat_date")
    private CsatDate csatDate;
}
