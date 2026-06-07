package com.gateflow.tracker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionResult {

    private List<RetentionCohort> cohorts;
    private List<RetentionCurvePoint> retentionCurve;
    private RetentionSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionCohort {
        private String cohortDate;
        private int initialUsers;
        private Map<String, Double> retentionRates;
        private Map<String, Integer> retentionCounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionCurvePoint {
        private int day;
        private double rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionSummary {
        private double day1Rate;
        private double day7Rate;
        private double day30Rate;
        private long totalInitialUsers;
    }
}
