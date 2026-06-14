package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.domain.entity.*;
import com.gateflow.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {

    private final TrackerEventMapper eventMapper;
    private final TrackerPropertyMapper propertyMapper;
    private final TrackerPlanMapper planMapper;
    private final TrackerDashboardMapper dashboardMapper;
    private static final ObjectMapper om = new ObjectMapper();

    public List<EventLineageVO> listEvents() {
        return eventMapper.selectList(new LambdaQueryWrapper<TrackerEvent>()
                .eq(TrackerEvent::getStatus, 1)
                .orderByDesc(TrackerEvent::getCreatedAt))
            .stream()
            .map(this::toEventLineageVO)
            .collect(Collectors.toList());
    }

    public EventLineageVO getEventLineage(String eventKey) {
        TrackerEvent event = eventMapper.selectOne(
            new LambdaQueryWrapper<TrackerEvent>().eq(TrackerEvent::getEventKey, eventKey));
        if (event == null) return null;
        return toEventLineageVO(event);
    }

    public LineageGraphVO getGraph(String eventKey) {
        TrackerEvent event = eventMapper.selectOne(
            new LambdaQueryWrapper<TrackerEvent>().eq(TrackerEvent::getEventKey, eventKey));
        if (event == null) return new LineageGraphVO(Collections.emptyList(), Collections.emptyList());

        List<LineageGraphVO.Node> nodes = new ArrayList<>();
        List<LineageGraphVO.Edge> edges = new ArrayList<>();

        // Event node (center)
        String eventNodeId = "event:" + event.getEventKey();
        nodes.add(new LineageGraphVO.Node(eventNodeId, event.getEventName(), "event", 40));

        // Properties linked to this event
        List<TrackerProperty> props = propertyMapper.selectList(
            new LambdaQueryWrapper<TrackerProperty>().eq(TrackerProperty::getEventId, event.getId()));
        for (TrackerProperty prop : props) {
            String propNodeId = "prop:" + prop.getPropKey();
            nodes.add(new LineageGraphVO.Node(propNodeId, prop.getPropName(), "property", 22));
            edges.add(new LineageGraphVO.Edge(eventNodeId, propNodeId, "has_prop"));
        }

        // Plans that reference this event (via events_json LIKE '%eventKey%')
        List<TrackerPlan> plans = planMapper.selectList(
            new LambdaQueryWrapper<TrackerPlan>().like(TrackerPlan::getEventsJson, eventKey));
        for (TrackerPlan plan : plans) {
            String planNodeId = "plan:" + plan.getId();
            nodes.add(new LineageGraphVO.Node(planNodeId, plan.getPlanName(), "dashboard", 28));
            edges.add(new LineageGraphVO.Edge(planNodeId, eventNodeId, "references"));
        }

        // Dashboards (if any reference events - placeholder for future)
        List<TrackerDashboard> dashboards = dashboardMapper.selectList(
            new LambdaQueryWrapper<TrackerDashboard>().like(TrackerDashboard::getConfig, eventKey));
        for (TrackerDashboard db : dashboards) {
            String dbNodeId = "dashboard:" + db.getId();
            nodes.add(new LineageGraphVO.Node(dbNodeId, db.getName(), "dashboard", 28));
            edges.add(new LineageGraphVO.Edge(dbNodeId, eventNodeId, "references"));
        }

        return new LineageGraphVO(nodes, edges);
    }

    private EventLineageVO toEventLineageVO(TrackerEvent event) {
        EventLineageVO vo = new EventLineageVO();
        vo.setEventKey(event.getEventKey());
        vo.setEventName(event.getEventName());
        vo.setCategory(event.getCategory());

        // Load properties
        List<TrackerProperty> props = propertyMapper.selectList(
            new LambdaQueryWrapper<TrackerProperty>().eq(TrackerProperty::getEventId, event.getId()));
        vo.setProperties(props.stream().map(p -> {
            EventLineageVO.PropItem pi = new EventLineageVO.PropItem();
            pi.setPropKey(p.getPropKey());
            pi.setPropName(p.getPropName());
            pi.setDataType(p.getDataType());
            return pi;
        }).collect(Collectors.toList()));

        // Build references from plans
        List<EventLineageVO.RefItem> refs = new ArrayList<>();
        List<TrackerPlan> plans = planMapper.selectList(
            new LambdaQueryWrapper<TrackerPlan>().like(TrackerPlan::getEventsJson, event.getEventKey()));
        for (TrackerPlan plan : plans) {
            EventLineageVO.RefItem ref = new EventLineageVO.RefItem();
            ref.setRefType("dashboard");
            ref.setRefId(plan.getId());
            ref.setRefName(plan.getPlanName());
            refs.add(ref);
        }

        // Also check dashboards
        List<TrackerDashboard> dashboards = dashboardMapper.selectList(
            new LambdaQueryWrapper<TrackerDashboard>().like(TrackerDashboard::getConfig, event.getEventKey()));
        for (TrackerDashboard db : dashboards) {
            EventLineageVO.RefItem ref = new EventLineageVO.RefItem();
            ref.setRefType("dashboard");
            ref.setRefId(db.getId());
            ref.setRefName(db.getName());
            refs.add(ref);
        }

        vo.setReferences(refs);
        return vo;
    }
}
