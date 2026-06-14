package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class EventLineageVO {
    private String eventKey;
    private String eventName;
    private String category;
    private List<RefItem> references = new ArrayList<>();
    private List<PropItem> properties = new ArrayList<>();

    @Data
    public static class RefItem {
        private String refType;
        private Long refId;
        private String refName;
    }

    @Data
    public static class PropItem {
        private String propKey;
        private String propName;
        private String dataType;
    }
}
