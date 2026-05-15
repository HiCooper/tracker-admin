package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SpmVO {
    private Long id;
    private String spmCode;
    private String spmName;
    private String spmaLabel;
    private String spmbLabel;
    private String spmcLabel;
    private String spmdLabel;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}