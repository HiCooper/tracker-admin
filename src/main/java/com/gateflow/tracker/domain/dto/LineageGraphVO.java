package com.gateflow.tracker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class LineageGraphVO {
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    @AllArgsConstructor
    public static class Node {
        private String id;
        private String name;
        private String type;
        private Integer symbolSize;
    }

    @Data
    @AllArgsConstructor
    public static class Edge {
        private String source;
        private String target;
        private String label;
    }
}
