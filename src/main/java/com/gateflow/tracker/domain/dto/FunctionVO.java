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
public class FunctionVO {
    private Long id;
    private Long blockId;
    private String funcCode;
    private String funcName;
    private LocalDateTime createdAt;
}
