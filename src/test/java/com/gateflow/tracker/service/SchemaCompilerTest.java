package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.dto.AppSchemaDto;
import com.gateflow.tracker.domain.dto.PlanVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaCompilerTest {

    private final SchemaCompiler compiler = new SchemaCompiler();

    private PlanVO.EventItem event(String key, PlanVO.PropItem... props) {
        PlanVO.EventItem e = new PlanVO.EventItem();
        e.setEventKey(key);
        e.setProperties(List.of(props));
        return e;
    }

    private PlanVO.PropItem prop(String key, String type) {
        PlanVO.PropItem p = new PlanVO.PropItem();
        p.setPropKey(key);
        p.setDataType(type);
        return p;
    }

    @Test
    void compilesEventsAndFields() {
        AppSchemaDto dto = compiler.compile("A_MAIN", 7,
                List.of(event("purchase", prop("orderId", "string"), prop("amount", "number"))));

        assertThat(dto.getAppId()).isEqualTo("A_MAIN");
        assertThat(dto.getVersion()).isEqualTo(7);
        assertThat(dto.getEvents()).containsKey("purchase");
        List<AppSchemaDto.FieldSpecDto> fields = dto.getEvents().get("purchase").getFields();
        assertThat(fields).extracting(AppSchemaDto.FieldSpecDto::getName)
                .containsExactly("orderId", "amount");
        assertThat(fields).allMatch(AppSchemaDto.FieldSpecDto::isRequired);
        assertThat(fields.get(1).getType()).isEqualTo("number");
    }

    @Test
    void normalizesDataTypes() {
        assertThat(SchemaCompiler.normalizeType("整数")).isEqualTo("integer");
        assertThat(SchemaCompiler.normalizeType("Float")).isEqualTo("number");
        assertThat(SchemaCompiler.normalizeType("BOOL")).isEqualTo("boolean");
        assertThat(SchemaCompiler.normalizeType("json")).isEqualTo("object");
        assertThat(SchemaCompiler.normalizeType(null)).isEqualTo("string");
        assertThat(SchemaCompiler.normalizeType("weird")).isEqualTo("string");
    }

    @Test
    void skipsEventsAndPropsWithoutKey() {
        AppSchemaDto dto = compiler.compile("A", 1,
                List.of(event(""), event("ok", prop("", "string"), prop("k", "string"))));
        assertThat(dto.getEvents()).containsOnlyKeys("ok");
        assertThat(dto.getEvents().get("ok").getFields()).extracting(AppSchemaDto.FieldSpecDto::getName)
                .containsExactly("k");
    }

    @Test
    void handlesNullEvents() {
        assertThat(compiler.compile("A", 1, null).getEvents()).isEmpty();
    }
}
