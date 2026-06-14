package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdatePlanRequest {
    private String planName;
    private String appVersion;
    private List<EventItem> events;

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
