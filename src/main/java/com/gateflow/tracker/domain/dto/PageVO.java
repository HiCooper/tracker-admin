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
public class PageVO {
    private Long id;
    private Long appId;
    private String appCode;
    private String pageCode;
    private String pageName;
    private Integer blockCount;
    private LocalDateTime createdAt;
}
