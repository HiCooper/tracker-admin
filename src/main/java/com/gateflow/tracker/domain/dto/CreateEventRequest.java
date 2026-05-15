package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEventRequest {

    @NotBlank(message = "事件标识不能为空")
    @Size(max = 64, message = "事件标识长度不能超过64")
    private String eventKey;

    @NotBlank(message = "事件名称不能为空")
    @Size(max = 128, message = "事件名称长度不能超过128")
    private String eventName;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    @Pattern(regexp = "^(page_view|click|exposure|custom)$", message = "分类只能是 page_view/click/exposure/custom")
    private String category = "custom";

    private Integer status = 1;
}