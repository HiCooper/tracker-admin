package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFunctionRequest {

    @NotBlank(message = "功能名称不能为空")
    private String funcName;

    @NotBlank(message = "功能标识不能为空")
    private String funcCode;
}
