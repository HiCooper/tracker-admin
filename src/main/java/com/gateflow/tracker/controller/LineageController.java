package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.LineageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lineage API", description = "埋点血缘追踪接口")
@RestController
@RequestMapping("/api/v1/engineering/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    @GetMapping("/events")
    @Operation(summary = "事件列表（含属性和引用摘要）")
    public ResponseEntity<ApiResponse<List<EventLineageVO>>> listEvents() {
        return ResponseEntity.ok(ApiResponse.success(lineageService.listEvents()));
    }

    @GetMapping("/events/{eventKey}")
    @Operation(summary = "事件血缘详情")
    public ResponseEntity<ApiResponse<EventLineageVO>> getEventLineage(
            @Parameter(description = "事件标识") @PathVariable String eventKey) {
        EventLineageVO vo = lineageService.getEventLineage(eventKey);
        if (vo == null) {
            return ResponseEntity.ok(ApiResponse.error(3001, "Event not found: " + eventKey));
        }
        return ResponseEntity.ok(ApiResponse.success(vo));
    }

    @GetMapping("/events/{eventKey}/graph")
    @Operation(summary = "血缘关系图")
    public ResponseEntity<ApiResponse<LineageGraphVO>> getGraph(
            @Parameter(description = "事件标识") @PathVariable String eventKey) {
        return ResponseEntity.ok(ApiResponse.success(lineageService.getGraph(eventKey)));
    }
}
