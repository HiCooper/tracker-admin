package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventVO {
    private Long id;
    private String eventKey;
    private String eventName;
    private String description;
    private String category;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}