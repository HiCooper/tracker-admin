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
public class PathResult {

    private List<PathNode> nodes;
    private List<PathTransition> transitions;
    private List<TopPath> topPaths;
    private PathSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathNode {
        private String name;
        private int value;
        private int depth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathTransition {
        private String source;
        private String target;
        private int count;
        private double rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPath {
        private List<String> path;
        private int count;
        private int users;
        private double rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathSummary {
        private long totalSessions;
        private double avgPathDepth;
    }
}
