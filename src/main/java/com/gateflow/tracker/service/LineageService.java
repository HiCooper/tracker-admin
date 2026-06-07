package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gateflow.tracker.domain.entity.TrackerEvent;
import com.gateflow.tracker.domain.entity.TrackerProperty;
import com.gateflow.tracker.repository.TrackerEventMapper;
import com.gateflow.tracker.repository.TrackerPropertyMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineageService {

    private final TrackerEventMapper eventMapper;
    private final TrackerPropertyMapper propertyMapper;

    @Data
    public static class EventLineageVO {
        private String eventKey;
        private String eventName;
        private String category;
        private List<Reference> references;
        private List<PropDef> properties;
    }

    @Data
    public static class Reference {
        private String refType;
        private Long refId;
        private String refName;
    }

    @Data
    public static class PropDef {
        private String propKey;
        private String propName;
        private String dataType;
    }

    @Data
    public static class LineageGraph {
        private List<GraphNode> nodes;
        private List<GraphEdge> edges;
    }

    @Data
    public static class GraphNode {
        private String id;
        private String name;
        private String type;
        private int symbolSize;
    }

    @Data
    public static class GraphEdge {
        private String source;
        private String target;
        private String label;
    }

    public List<EventLineageVO> listEventLineages() {
        LambdaQueryWrapper<TrackerEvent> qw = Wrappers.lambdaQuery();
        qw.eq(TrackerEvent::getStatus, 1);
        List<TrackerEvent> events = eventMapper.selectList(qw);

        return events.stream().map(evt -> {
            EventLineageVO vo = new EventLineageVO();
            vo.setEventKey(evt.getEventKey());
            vo.setEventName(evt.getEventName());
            vo.setCategory(evt.getCategory());
            vo.setReferences(buildReferences(evt));
            vo.setProperties(buildProperties(evt.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    public EventLineageVO getEventLineage(String eventKey) {
        LambdaQueryWrapper<TrackerEvent> qw = Wrappers.lambdaQuery();
        qw.eq(TrackerEvent::getEventKey, eventKey);
        TrackerEvent evt = eventMapper.selectOne(qw);
        if (evt == null) return null;

        EventLineageVO vo = new EventLineageVO();
        vo.setEventKey(evt.getEventKey());
        vo.setEventName(evt.getEventName());
        vo.setCategory(evt.getCategory());
        vo.setReferences(buildReferences(evt));
        vo.setProperties(buildProperties(evt.getId()));
        return vo;
    }

    public LineageGraph getEventGraph(String eventKey) {
        EventLineageVO lineage = getEventLineage(eventKey);
        if (lineage == null) return null;

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        nodes.add(buildNode(eventKey, lineage.getEventName(), "event", 40));

        for (PropDef prop : lineage.getProperties()) {
            nodes.add(buildNode(prop.getPropKey(), prop.getPropName(), "property", 20));
            edges.add(buildEdge(eventKey, prop.getPropKey(), "包含"));
        }

        for (Reference ref : lineage.getReferences()) {
            String refId = ref.getRefType() + "_" + ref.getRefId();
            nodes.add(buildNode(refId, ref.getRefName(), ref.getRefType(), 30));
            edges.add(buildEdge(refId, eventKey, "使用"));
        }

        LineageGraph graph = new LineageGraph();
        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }

    private List<Reference> buildReferences(TrackerEvent evt) {
        return new ArrayList<>();
    }

    private List<PropDef> buildProperties(Long eventId) {
        LambdaQueryWrapper<TrackerProperty> qw = Wrappers.lambdaQuery();
        qw.eq(TrackerProperty::getEventId, eventId);
        return propertyMapper.selectList(qw).stream().map(prop -> {
            PropDef def = new PropDef();
            def.setPropKey(prop.getPropKey());
            def.setPropName(prop.getPropName());
            def.setDataType(prop.getDataType());
            return def;
        }).collect(Collectors.toList());
    }

    private GraphNode buildNode(String id, String name, String type, int size) {
        GraphNode node = new GraphNode();
        node.setId(id);
        node.setName(name);
        node.setType(type);
        node.setSymbolSize(size);
        return node;
    }

    private GraphEdge buildEdge(String source, String target, String label) {
        GraphEdge edge = new GraphEdge();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setLabel(label);
        return edge;
    }
}
