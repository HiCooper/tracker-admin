package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDashboardRequest {

    @Size(max = 128, message = "看板名称长度不能超过128")
    private String name;

    private String config;

    @Min(value = 0, message = "状态格式不正确")
    @Max(value = 1, message = "状态格式不正确")
    private Integer status;
}