package com.gateflow.tracker.domain.dto.advanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 路径分析请求与结果。 */
public class PathDto {

    @Data
    public static class Request {
        private String startPage;
        private String positiveEvent;
        private int depth;
        private String startTime;
        private String endTime;
        private String platform;
        private String appCode;
        private Integer minTransitionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transition {
        private String source;
        private String target;
        private long count;
        private double rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String name;
        private long value;
        private int depth;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPath {
        private List<String> path;
        private long count;
        private long users;
        private double rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalSessions;
        private double avgPathDepth;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private List<Node> nodes;
        private List<Transition> transitions;
        private List<TopPath> topPaths;
        private Summary summary;
    }
}
