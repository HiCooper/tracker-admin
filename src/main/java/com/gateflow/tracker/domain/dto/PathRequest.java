package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PathRequest {

    private String startPage;
    private String positiveEvent;

    @NotNull(message = "路径深度不能为空")
    private Integer depth;

    private String startTime;
    private String endTime;
    private String platform;
    private String appCode;
    private Integer minTransitionCount;

    public Integer getDepth() { return depth != null ? depth : 5; }
    public Integer getMinTransitionCount() { return minTransitionCount != null ? minTransitionCount : 10; }
}
