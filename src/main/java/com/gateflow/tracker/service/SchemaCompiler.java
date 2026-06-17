package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.dto.AppSchemaDto;
import com.gateflow.tracker.domain.dto.PlanVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把埋点方案(Plan)的事件定义编译为可发布的 app 事件契约(AppSchemaDto)。
 */
@Component
public class SchemaCompiler {

    public AppSchemaDto compile(String appCode, long version, List<PlanVO.EventItem> events) {
        Map<String, AppSchemaDto.EventSchemaDto> eventMap = new LinkedHashMap<>();
        if (events != null) {
            for (PlanVO.EventItem ev : events) {
                if (ev == null || !StringUtils.hasText(ev.getEventKey())) {
                    continue;
                }
                List<AppSchemaDto.FieldSpecDto> fields = new ArrayList<>();
                if (ev.getProperties() != null) {
                    for (PlanVO.PropItem p : ev.getProperties()) {
                        if (p == null || !StringUtils.hasText(p.getPropKey())) {
                            continue;
                        }
                        // 方案声明的属性视为期望字段(required=true),用于漂移检测
                        fields.add(new AppSchemaDto.FieldSpecDto(
                                p.getPropKey(), normalizeType(p.getDataType()), true));
                    }
                }
                eventMap.put(ev.getEventKey(), new AppSchemaDto.EventSchemaDto(fields));
            }
        }
        return new AppSchemaDto(appCode, version, eventMap);
    }

    /** 把方案里的数据类型归一为契约类型(string/number/integer/boolean/object/array)。 */
    static String normalizeType(String dataType) {
        if (!StringUtils.hasText(dataType)) {
            return "string";
        }
        return switch (dataType.trim().toLowerCase()) {
            case "string", "str", "text", "字符串" -> "string";
            case "number", "float", "double", "decimal", "数字" -> "number";
            case "int", "integer", "long", "整数" -> "integer";
            case "bool", "boolean", "布尔" -> "boolean";
            case "object", "json", "map", "对象" -> "object";
            case "array", "list", "数组" -> "array";
            default -> "string";
        };
    }
}
