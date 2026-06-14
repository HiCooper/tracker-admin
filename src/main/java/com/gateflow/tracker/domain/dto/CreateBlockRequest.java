package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBlockRequest {

    @NotBlank(message = "模块名称不能为空")
    private String blockName;

    @NotBlank(message = "模块标识不能为空")
    private String blockCode;
}
