package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDashboardRequest {

    @NotBlank(message = "看板名称不能为空")
    @Size(max = 128, message = "看板名称长度不能超过128")
    private String name;

    private String config = "{}";

    private Integer status = 1;

    private String createdBy;
}