package com.gateflow.tracker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {

    private String status;  // UP | DOWN | DEGRADED

    private Map<String, ComponentStatus> components;

    private String uptime;

    private String version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentStatus {
        private String status;
        private Long latency;
        private String detail;
    }
}
