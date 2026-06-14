package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlanVO {
    private Long id;
    private String planName;
    private Long appId;
    private String appName;
    private String appVersion;
    private String status;
    private List<EventItem> events;
    private String submitter;
    private String reviewer;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class EventItem {
        private String eventKey;
        private String eventName;
        private String category;
        private String description;
        private String spmCode;
        private List<PropItem> properties;
    }

    @Data
    public static class PropItem {
        private String propKey;
        private String propName;
        private String dataType;
    }
}
