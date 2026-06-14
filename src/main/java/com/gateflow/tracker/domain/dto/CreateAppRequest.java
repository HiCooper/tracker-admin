package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAppRequest {

    @NotBlank(message = "应用名称不能为空")
    private String appName;

    @NotBlank(message = "应用标识不能为空")
    private String appCode;

    private String description;
}
