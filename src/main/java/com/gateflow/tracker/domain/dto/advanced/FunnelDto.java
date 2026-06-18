package com.gateflow.tracker.domain.dto.advanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 漏斗分析请求与结果(字段名与前端 advancedAnalysis.ts 对齐)。 */
public class FunnelDto {

    @Data
    public static class Request {
        private List<StepDef> steps;
        private String startTime;
        private String endTime;
        private int conversionWindowMinutes;
        private String platform;
        private String appCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDef {
        private String stepName;
        private String eventType;
        private String eventFilter;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private int stepIndex;
        private String stepName;
        private String eventType;
        private String eventFilter;
        private long count;
        private long users;
        private double conversionRate;
        private double stepConversionRate;
        private double medianDurationSec;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendStep {
        private int stepIndex;
        private long count;
        private double conversionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private List<TrendStep> steps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private List<Step> steps;
        private double overallConversionRate;
        private long totalEntrants;
        private List<TrendPoint> trend;
    }
}
