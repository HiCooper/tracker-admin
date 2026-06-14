package com.gateflow.tracker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewPlanRequest {
    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String comment;
}
