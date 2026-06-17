package com.gateflow.tracker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 发布到 Redis 的 app 事件契约,字段名须与 tracker-service 的 AppSchema 对齐。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSchemaDto {

    private String appId;
    private long version;
    private Map<String, EventSchemaDto> events;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSchemaDto {
        private List<FieldSpecDto> fields;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldSpecDto {
        private String name;
        private String type;
        private boolean required;
    }
}
