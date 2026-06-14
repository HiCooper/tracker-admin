package com.gateflow.tracker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVO {
    private Long id;
    private String appCode;
    private String appName;
    private String description;
    private Integer pageCount;
    private LocalDateTime createdAt;
}
