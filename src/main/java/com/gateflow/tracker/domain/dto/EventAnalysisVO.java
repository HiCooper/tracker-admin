package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EventAnalysisVO {
    private LocalDate date;
    private Integer hour;
    private String platform;
    private String eventType;
    private Long eventCount;
    private Long userCount;
    private Long deviceCount;
}