package com.CBTServer.WebCSAT.service;

import java.util.List;
import java.util.Map;

public interface StatsService {
    List<Map<String, Object>> getPercentileTrend(Long userId, String groupBy);
    Map<String, Object> getScoreDistribution(String csatDate, Long subclassId, Long subjectId, String scope, int bin, Long userId);
    Map<String, Object> getRetakeRate(String csatDate);
    Map<String, Object> getExamKpis(String csatDate, Long subclassId, Long subjectId, String scope);
}
