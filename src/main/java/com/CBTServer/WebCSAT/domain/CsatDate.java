package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "csat_date")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CsatDate {
    @Id
    @Column(name = "csat_date", nullable = false)
    private String csatDate;

    @OneToMany(mappedBy = "csatDate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @OneToMany(mappedBy = "csatDate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exam> exams;


    @OneToMany(mappedBy = "csatDate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageMeta> imageMetas;
}
