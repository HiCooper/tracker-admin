package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEventRequest {

    @Size(max = 128, message = "事件名称长度不能超过128")
    private String eventName;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    @Pattern(regexp = "^(page_view|click|exposure|custom)$", message = "分类只能是 page_view/click/exposure/custom")
    private String category;

    @Min(value = 0, message = "状态只能是 0禁用 1启用")
    @Max(value = 1, message = "状态只能是 0禁用 1启用")
    private Integer status;
}