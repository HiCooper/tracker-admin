package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSpmRequest {

    @NotBlank(message = "SPM编码不能为空")
    @Size(max = 64, message = "SPM编码长度不能超过64")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "SPM编码只能包含大写字母、数字、下划线")
    private String spmCode;

    @NotBlank(message = "SPM名称不能为空")
    @Size(max = 128, message = "SPM名称长度不能超过128")
    private String spmName;

    @Size(max = 64, message = "A层标签长度不能超过64")
    private String spmaLabel;

    @Size(max = 64, message = "B层标签长度不能超过64")
    private String spmbLabel;

    @Size(max = 64, message = "C层标签长度不能超过64")
    private String spmcLabel;

    @Size(max = 64, message = "D层标签长度不能超过64")
    private String spmdLabel;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    private Integer status;
}