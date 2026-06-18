package com.gateflow.tracker.domain.dto.advanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** 留存分析请求与结果。 */
public class RetentionDto {

    @Data
    public static class Request {
        private String initialEvent;
        private String returnEvent;
        private String startTime;
        private String endTime;
        private List<Integer> retentionDays;
        private String platform;
        private String appCode;
        private String groupBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cohort {
        private String cohortDate;
        private long initialUsers;
        private Map<String, Double> retentionRates;
        private Map<String, Long> retentionCounts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurvePoint {
        private int day;
        private double rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private double day1Rate;
        private double day7Rate;
        private double day30Rate;
        private long totalInitialUsers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private List<Cohort> cohorts;
        private List<CurvePoint> retentionCurve;
        private Summary summary;
    }
}
