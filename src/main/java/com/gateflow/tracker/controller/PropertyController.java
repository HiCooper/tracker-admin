package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gateflow.tracker.security.RequireRole;

import java.util.List;

@Tag(name = "Property API", description = "属性管理接口")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping("/events/{eventId}/properties")
    @Operation(summary = "获取事件属性列表")
    public ResponseEntity<ApiResponse<List<PropertyVO>>> getPropertiesByEventId(
            @Parameter(description = "事件ID") @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getPropertiesByEventId(eventId)));
    }

    @PostMapping("/properties")
    @Operation(summary = "创建属性")
    public ResponseEntity<ApiResponse<PropertyVO>> createProperty(
            @Valid @RequestBody CreatePropertyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.createProperty(request)));
    }

    @RequireRole("admin")
    @DeleteMapping("/properties/{id}")
    @Operation(summary = "删除属性")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @Parameter(description = "属性ID") @PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}