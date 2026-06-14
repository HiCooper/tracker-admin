package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreatePlanRequest {
    @NotBlank(message = "方案名称不能为空")
    private String planName;

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    private String appName;

    @NotBlank(message = "版本号不能为空")
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
