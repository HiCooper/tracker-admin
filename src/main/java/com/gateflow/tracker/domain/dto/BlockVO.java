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
public class BlockVO {
    private Long id;
    private Long pageId;
    private String blockCode;
    private String blockName;
    private Integer functionCount;
    private LocalDateTime createdAt;
}
