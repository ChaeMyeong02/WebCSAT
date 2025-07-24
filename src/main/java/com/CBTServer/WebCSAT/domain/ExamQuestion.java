package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam_question")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExamQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_id", updatable = false, unique = true)
    private Long joinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", referencedColumnName = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id", referencedColumnName = "subclass_id")
    private Subclass subclass;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "selected_answer")
    private String selectedAnswer;

    @Column(name = "is_correct")
    private boolean isCorrect;

}
