package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EventAnalysisQueryRequest {
    private String eventKey;
    private LocalDate startDate;
    private LocalDate endDate;
    private String platform;
}