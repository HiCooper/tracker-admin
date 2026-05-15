package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePropertyRequest {

    @NotNull(message = "事件ID不能为空")
    private Long eventId;

    @NotBlank(message = "属性标识不能为空")
    @Size(max = 64, message = "属性标识长度不能超过64")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "属性标识格式不正确")
    private String propKey;

    @NotBlank(message = "属性名称不能为空")
    @Size(max = 128, message = "属性名称长度不能超过128")
    private String propName;

    @Pattern(regexp = "^(string|number|boolean|date)$", message = "数据类型只能是 string/number/boolean/date")
    private String dataType = "string";

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;
}