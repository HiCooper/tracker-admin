package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SessionAnalysisVO {
    private LocalDate date;
    private Integer hour;
    private String platform;
    private Long sessionCount;
    private Long userCount;
    private BigDecimal avgDuration;
    private BigDecimal avgPageDepth;
    private Long bounceCount;
    private BigDecimal bounceRate;
}