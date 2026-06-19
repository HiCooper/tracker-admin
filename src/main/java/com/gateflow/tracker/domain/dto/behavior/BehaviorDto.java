package com.gateflow.tracker.domain.dto.behavior;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 行为分析 DTO,字段名与前端 behaviorApi.ts 对齐。率为分数(0..1),trend 为百分数。 */
public class BehaviorDto {

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class EventSummary {
        private String eventType; private long count, users; private double avgPerUser, trend;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class BehaviorOverview {
        private long totalEvents, eventTypeCount, activeUsers; private double avgEventsPerUser;
        private List<EventSummary> topEvents;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class FunnelStep { private String step; private long users; private double rate; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class FunnelData {
        private List<FunnelStep> steps; private long totalEntered;
        private double overallConversionRate; private String maxDropStep; private double medianConversionMinutes;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PathNode { private String page; private long sessions; private double percentage; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PathTransition { private String from; private String to; private long count; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PathData {
        private List<PathNode> nodes; private List<PathTransition> transitions;
        private long totalSessions; private double avgDepth; private long pageCount;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RetentionCohort { private String date; private long users; private List<Double> rates; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RetentionData {
        private List<RetentionCohort> cohorts; private double day2Rate, day7Rate, day30Rate;
    }
}
