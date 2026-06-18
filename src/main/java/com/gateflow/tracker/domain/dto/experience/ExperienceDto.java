package com.gateflow.tracker.domain.dto.experience;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 体验分析 DTO,字段名与前端 experienceApi.ts 对齐。 */
public class ExperienceDto {

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class HeatmapPoint { private int x, y; private long count; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class HeatmapData {
        private String pageUrl;
        private int viewportWidth, viewportHeight;
        private long totalClicks;
        private List<HeatmapPoint> points;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PortraitDimension {
        private String label, value; private long count; private double percentage; // 百分数
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ActiveHour { private int hour, dayOfWeek; private long count; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class UserPortrait {
        private List<PortraitDimension> deviceType, os, browser, language, screenResolution, source;
        private List<ActiveHour> activeHours;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PageListItem { private String pageUrl, pageTitle; private long pageViews; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SessionRecord {
        private String id, user, device, os; private long pages; private String dur, ts;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ConversionStep { private String step; private long users; private double rate; }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AnalysisReport { private String name, period, type, status, ts; }
}
