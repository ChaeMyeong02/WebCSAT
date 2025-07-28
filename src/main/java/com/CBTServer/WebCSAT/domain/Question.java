package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question")
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id", updatable = false, unique = true)
    private Long questionId;

    @Column(name = "num")
    private int num;

    @Lob
    @Column(name = "question_title", columnDefinition = "TEXT")
    private String questionTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_article", referencedColumnName = "url")
    private ImageMeta questionArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_context", referencedColumnName = "url")
    private ImageMeta questionContext;

    @Column(name = "score")
    private int score;

    @Column(name = "answer")
    private String answer;

    @Lob
    @Column(name = "option1", columnDefinition = "TEXT")
    private String option1;

    @Lob
    @Column(name = "option2", columnDefinition = "TEXT")
    private String option2;

    @Lob
    @Column(name = "option3", columnDefinition = "TEXT")
    private String option3;

    @Lob
    @Column(name = "option4", columnDefinition = "TEXT")
    private String option4;

    @Lob
    @Column(name = "option5", columnDefinition = "TEXT")
    private String option5;

    @Column(name = "question_type")
    private boolean questionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "csat_date", referencedColumnName = "csat_date")
    private CsatDate csatDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", referencedColumnName = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id", referencedColumnName = "subclass_id")
    private Subclass subclass;
}

