package com.gateflow.tracker.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 看板取数结果,字段名与前端 dashboardApi.ts 对齐。 */
public class DashboardDataDto {

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PanelData {
        private String panelId;
        private String type;
        private String title;
        private Object result;
        private String error;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class DashboardDataResult {
        private long dashboardId;
        private String name;
        private List<PanelData> panels;
    }
}
