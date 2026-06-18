package com.gateflow.tracker.domain.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 平台数据 overview 的 DTO,字段名与前端 platformDataApi.ts 对齐。 */
public class PlatformDto {

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CoreMetrics {
        private long uv, sessions, pv, newUsers;
        private double avgDuration, avgDepth, bounceRate, conversionRate; // 时长=秒,率=百分数
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ChannelBreakdown {
        private String name; private long uv; private int rank;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PageBreakdown {
        private String name; private long pv; private int rank;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class TopPage {
        private String name; private long count;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RealtimeSnapshot {
        private long online, todayUv, todaySessions, todayNewUsers;
        private List<TopPage> topPages;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ChannelDetail {
        private String channel; private long uv, newUv, sessions;
        private double avgDuration, bounceRate; // 秒 / 百分数
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PageDetail {
        private String path; private long uv, pv, entry, exit;
        private double exitRate, avgStay; // 百分数 / 秒
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RetentionSummary {
        private double day1Rate, day7Rate, day30Rate, activeDay7Rate; // 分数 0..1
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CohortRow {
        private String date; private long users; private List<Double> rates; // 分数 0..1
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RetentionResult {
        private RetentionSummary summary; private List<CohortRow> cohorts;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AnomalyItem {
        private String metric, change, dir, detail; // dir: up|down
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AnalysisOverview {
        private long dau, mau;
        private double avgSessionsPerUser, avgDuration, avgPagesPerSession, day7Retention; // 秒 / 百分数
        private List<ChannelDetail> channels;
        private List<PageDetail> pages;
    }
}
