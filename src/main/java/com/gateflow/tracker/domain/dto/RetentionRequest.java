package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RetentionRequest {

    private String initialEvent;
    private String returnEvent;

    private String startTime;
    private String endTime;

    @NotEmpty(message = "留存天数不能为空")
    private List<Integer> retentionDays;

    private String platform;
    private String appCode;
    private String groupBy;

    public String getGroupBy() {
        return groupBy != null ? groupBy : "day";
    }
}
