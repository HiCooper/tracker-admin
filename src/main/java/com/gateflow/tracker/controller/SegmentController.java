package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.entity.TrackerSegment;
import com.gateflow.tracker.service.SegmentService;
import com.gateflow.tracker.service.SegmentService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Segment API", description = "用户分群管理")
@RestController
@RequestMapping("/api/v1/segments")
@RequiredArgsConstructor
public class SegmentController {

    private final SegmentService segmentService;

    @GetMapping
    @Operation(summary = "分群列表")
    public ResponseEntity<ApiResponse<List<TrackerSegment>>> list() {
        return ResponseEntity.ok(ApiResponse.success(segmentService.list()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "分群详情")
    public ResponseEntity<ApiResponse<TrackerSegment>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.get(id)));
    }

    @PostMapping
    @Operation(summary = "创建分群")
    public ResponseEntity<ApiResponse<TrackerSegment>> create(@RequestBody SegmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分群")
    public ResponseEntity<ApiResponse<TrackerSegment>> update(@PathVariable Long id, @RequestBody SegmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分群")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        segmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PostMapping("/{id}/estimate")
    @Operation(summary = "估算分群人数")
    public ResponseEntity<ApiResponse<SegmentEstimate>> estimate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.estimateSize(id)));
    }
}
