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

    @Column(name = "listening_url")
    private String listeningUrl;

    @Column(name = "cut1")
    private Integer cut1;

    @Column(name = "cut2")
    private Integer cut2;

    @Column(name = "cut3")
    private Integer cut3;

    @Column(name = "cut4")
    private Integer cut4;

    @Column(name = "cut5")
    private Integer cut5;

    @Column(name = "cut6")
    private Integer cut6;

    @Column(name = "cut7")
    private Integer cut7;

    @Column(name = "cut8")
    private Integer cut8;

    @Column(name = "cut9")
    private Integer cut9;

    public SubclassCsatDate(Subclass subclass, CsatDate csatDate) {
        this.subclass = subclass;
        this.csatDate = csatDate;
    }
}
