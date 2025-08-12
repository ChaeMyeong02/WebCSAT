package com.CBTServer.WebCSAT.repository.proj;

import java.time.LocalDateTime;

public interface UserLatestAttemptRow {
    Long getExamId();
    String getCsatDate();
    Long getSubclassId();
    Integer getScore();
    LocalDateTime getTestDate();
}