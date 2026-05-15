package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DashboardVO {
    private Long id;
    private String name;
    private String config;
    private String createdBy;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}