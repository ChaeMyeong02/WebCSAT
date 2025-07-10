package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question_article")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class QuestionArticle {
    @Id
    @Column(name = "question_article", updatable = false, unique = true)
    private String questionArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "csat_date", referencedColumnName = "csat_date")
    private CsatDate csatDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id", referencedColumnName = "subclass_id")
    private Subclass subclass;
}
