package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/* Subclass Table */
@Entity
@Table(name = "subclass")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Subclass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subclass_id", updatable = false, unique = true)
    private Long subclassId;

    @Column(name = "subclass_name", nullable = false, unique = true)
    private String subclassName;

    @Column(name = "count")
    private int count;

    @Column(name = "optional")
    private boolean optional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToMany(mappedBy = "subclass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @OneToMany(mappedBy = "subclass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exam> exams;

    @OneToMany(mappedBy = "subclass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamQuestion> examQuestions;

    @OneToMany(mappedBy = "subclass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubclassCsatDate> subclassCsatDates;

    @OneToMany(mappedBy = "subclass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageMeta> imageMetas;
}
