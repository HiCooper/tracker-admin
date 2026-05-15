package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PropertyVO {
    private Long id;
    private Long eventId;
    private String eventName;
    private String propKey;
    private String propName;
    private String dataType;
    private String description;
    private LocalDateTime createdAt;
}