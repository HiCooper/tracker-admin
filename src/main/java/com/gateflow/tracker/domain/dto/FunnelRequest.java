package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FunnelRequest {

    @NotEmpty(message = "步骤列表不能为空")
    private List<FunnelStepDef> steps;

    private String startTime;
    private String endTime;

    @NotNull(message = "转化窗口不能为空")
    private Integer conversionWindowMinutes;

    private String platform;
    private String appCode;

    @Data
    public static class FunnelStepDef {
        private String stepName;
        private String eventType;
        private String eventFilter;
    }
}
