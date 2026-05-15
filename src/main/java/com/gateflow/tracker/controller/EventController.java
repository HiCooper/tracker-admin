package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Event API", description = "事件管理接口")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "查询事件列表")
    public ResponseEntity<PageResponse<EventVO>> listEvents(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "排序字段") @RequestParam(required = false) String sort,
            @Parameter(description = "排序方向") @RequestParam(required = false) String order) {
        return ResponseEntity.ok(eventService.listEvents(page, size, sort, order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取事件详情")
    public ResponseEntity<ApiResponse<EventVO>> getEvent(
            @Parameter(description = "事件ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventById(id)));
    }

    @PostMapping
    @Operation(summary = "创建事件")
    public ResponseEntity<ApiResponse<EventVO>> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success(eventService.createEvent(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新事件")
    public ResponseEntity<ApiResponse<EventVO>> updateEvent(
            @Parameter(description = "事件ID") @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success(eventService.updateEvent(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除事件")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @Parameter(description = "事件ID") @PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}