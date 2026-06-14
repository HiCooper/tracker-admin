package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePageRequest {

    @NotBlank(message = "页面名称不能为空")
    private String pageName;

    @NotBlank(message = "页面标识不能为空")
    private String pageCode;
}
