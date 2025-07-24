package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Time;
import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "exam")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id", updatable = false, unique = true)
    private Long examId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id", referencedColumnName = "subclass_id")
    private Subclass subclass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "csat_date", referencedColumnName = "csat_date")
    private CsatDate csatDate;

    @Column(name = "result")
    private int result;

    @CreatedDate
    @Column(name = "test_date")
    private LocalDateTime testDate;

    @Column(name = "duration")
    private Time duration;
}