package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;
import com.CBTServer.WebCSAT.domain.Subclass;

@Entity
@Table(name = "exam_question")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_id")
    private Long joinId;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "subclass_id")
    private Subclass subclass;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    private String selectedAnswer;

    private Boolean isCorrect;
}
