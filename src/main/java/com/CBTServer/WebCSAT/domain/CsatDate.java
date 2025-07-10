package com.CBTServer.WebCSAT.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
