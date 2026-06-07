package com.gateflow.tracker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelResult {

    private List<FunnelStep> steps;
    private double overallConversionRate;
    private long totalEntrants;
    private List<FunnelTrendPoint> trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStep {
        private int stepIndex;
        private String stepName;
        private String eventType;
        private String eventFilter;
        private long count;
        private long users;
        private double conversionRate;
        private double stepConversionRate;
        private long medianDurationSec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelTrendPoint {
        private String date;
        private List<StepCount> steps;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class StepCount {
            private int stepIndex;
            private long count;
            private double conversionRate;
        }
    }
}
