package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.service.LineageService;
import com.gateflow.tracker.service.LineageService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lineage API", description = "埋点血缘分析接口")
@RestController
@RequestMapping("/api/v1/engineering/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    @GetMapping("/events")
    @Operation(summary = "事件血缘列表")
    public ResponseEntity<ApiResponse<List<EventLineageVO>>> listLineages() {
        return ResponseEntity.ok(ApiResponse.success(lineageService.listEventLineages()));
    }

    @GetMapping("/events/{eventKey}")
    @Operation(summary = "事件血缘详情")
    public ResponseEntity<ApiResponse<EventLineageVO>> getLineage(@PathVariable String eventKey) {
        EventLineageVO vo = lineageService.getEventLineage(eventKey);
        if (vo == null) {
            return ResponseEntity.ok(ApiResponse.error(4002, "事件不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(vo));
    }

    @GetMapping("/events/{eventKey}/graph")
    @Operation(summary = "事件血缘图谱")
    public ResponseEntity<ApiResponse<LineageGraph>> getGraph(@PathVariable String eventKey) {
        LineageGraph graph = lineageService.getEventGraph(eventKey);
        if (graph == null) {
            return ResponseEntity.ok(ApiResponse.error(4002, "事件不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(graph));
    }
}
