package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gateflow.tracker.domain.dto.PageResponse.PageData;
import com.gateflow.tracker.domain.entity.TrackerPlan;
import com.gateflow.tracker.domain.entity.TrackerPlanEvent;
import com.gateflow.tracker.domain.entity.TrackerPlanProperty;
import com.gateflow.tracker.repository.TrackerPlanEventMapper;
import com.gateflow.tracker.repository.TrackerPlanMapper;
import com.gateflow.tracker.repository.TrackerPlanPropertyMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final TrackerPlanMapper planMapper;
    private final TrackerPlanEventMapper planEventMapper;
    private final TrackerPlanPropertyMapper planPropertyMapper;

    // ==================== DTOs ====================

    @Data
    public static class PlanCreateRequest {
        private String planName;
        private Long appId;
        private String appName;
        private String appVersion;
        private List<EventDef> events;
    }

    @Data
    public static class PlanUpdateRequest {
        private String planName;
        private Long appId;
        private String appName;
        private String appVersion;
        private List<EventDef> events;
    }

    @Data
    public static class PlanReviewRequest {
        private String action;
        private String comment;
    }

    @Data
    public static class EventDef {
        private String eventKey;
        private String eventName;
        private String category;
        private String description;
        private String spmCode;
        private List<PropDef> properties;
    }

    @Data
    public static class PropDef {
        private String propKey;
        private String propName;
        private String dataType;
    }

    @Data
    public static class PlanVO {
        private Long id;
        private String planName;
        private Long appId;
        private String appName;
        private String appVersion;
        private String status;
        private String submitter;
        private String reviewer;
        private String reviewComment;
        private List<EventVO> events;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class EventVO {
        private String eventKey;
        private String eventName;
        private String category;
        private String description;
        private String spmCode;
        private List<PropVO> properties;
    }

    @Data
    public static class PropVO {
        private String propKey;
        private String propName;
        private String dataType;
    }

    // ==================== Plan CRUD ====================

    public PageData<PlanVO> listPlans(int page, int size) {
        LambdaQueryWrapper<TrackerPlan> qw = Wrappers.lambdaQuery();
        qw.orderByDesc(TrackerPlan::getUpdatedAt);
        IPage<TrackerPlan> result = planMapper.selectPage(new Page<>(page, size), qw);

        List<PlanVO> vos = result.getRecords().stream()
                .map(p -> {
                    PlanVO vo = toPlanVO(p);
                    vo.setEvents(new ArrayList<>());
                    return vo;
                })
                .collect(Collectors.toList());

        PageData<PlanVO> pageData = new PageData<>();
        pageData.setList(vos);
        pageData.setTotal(result.getTotal());
        pageData.setPage((int) result.getCurrent());
        pageData.setSize((int) result.getSize());
        pageData.setPages((int) result.getPages());
        return pageData;
    }

    public PlanVO getPlan(Long id) {
        TrackerPlan plan = planMapper.selectById(id);
        if (plan == null) return null;
        return toPlanVOWithEvents(plan);
    }

    @Transactional
    public PlanVO createPlan(PlanCreateRequest req) {
        TrackerPlan plan = new TrackerPlan();
        plan.setPlanName(req.getPlanName());
        plan.setAppId(req.getAppId() != null ? req.getAppId() : 1L);
        plan.setAppName(req.getAppName() != null ? req.getAppName() : "");
        plan.setAppVersion(req.getAppVersion() != null ? req.getAppVersion() : "1.0");
        plan.setStatus("draft");
        plan.setSubmitter("current_user");
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.insert(plan);

        if (req.getEvents() != null) {
            saveEvents(plan.getId(), req.getEvents());
        }

        log.info("Plan created: id={}, name={}", plan.getId(), plan.getPlanName());
        return toPlanVOWithEvents(planMapper.selectById(plan.getId()));
    }

    @Transactional
    public PlanVO updatePlan(Long id, PlanUpdateRequest req) {
        TrackerPlan plan = planMapper.selectById(id);
        if (plan == null) throw new RuntimeException("方案不存在");

        if (req.getPlanName() != null) plan.setPlanName(req.getPlanName());
        if (req.getAppId() != null) plan.setAppId(req.getAppId());
        if (req.getAppName() != null) plan.setAppName(req.getAppName());
        if (req.getAppVersion() != null) plan.setAppVersion(req.getAppVersion());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);

        if (req.getEvents() != null) {
            LambdaQueryWrapper<TrackerPlanEvent> eqw = Wrappers.lambdaQuery();
            eqw.eq(TrackerPlanEvent::getPlanId, id);
            List<TrackerPlanEvent> oldEvents = planEventMapper.selectList(eqw);
            for (TrackerPlanEvent evt : oldEvents) {
                LambdaQueryWrapper<TrackerPlanProperty> pqw = Wrappers.lambdaQuery();
                pqw.eq(TrackerPlanProperty::getEventId, evt.getId());
                planPropertyMapper.delete(pqw);
            }
            planEventMapper.delete(eqw);
            saveEvents(id, req.getEvents());
        }

        return toPlanVOWithEvents(planMapper.selectById(id));
    }

    @Transactional
    public void deletePlan(Long id) {
        TrackerPlan plan = planMapper.selectById(id);
        if (plan == null) throw new RuntimeException("方案不存在");
        planMapper.deleteById(id);
    }

    @Transactional
    public PlanVO submitForReview(Long id) {
        TrackerPlan plan = planMapper.selectById(id);
        if (plan == null) throw new RuntimeException("方案不存在");
        if (!"draft".equals(plan.getStatus()) && !"rejected".equals(plan.getStatus())) {
            throw new RuntimeException("仅草稿或已驳回状态可提交审核");
        }
        plan.setStatus("reviewing");
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
        log.info("Plan {} submitted for review", id);
        return toPlanVOWithEvents(plan);
    }

    @Transactional
    public PlanVO reviewPlan(Long id, PlanReviewRequest req) {
        TrackerPlan plan = planMapper.selectById(id);
        if (plan == null) throw new RuntimeException("方案不存在");
        if (!"reviewing".equals(plan.getStatus())) {
            throw new RuntimeException("仅审核中状态可进行审核操作");
        }
        String action = req.getAction();
        if ("approve".equals(action)) {
            plan.setStatus("approved");
        } else if ("reject".equals(action)) {
            plan.setStatus("rejected");
        } else {
            throw new RuntimeException("无效的审核动作: " + action);
        }
        plan.setReviewer("admin");
        plan.setReviewComment(req.getComment());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
        log.info("Plan {} reviewed: {}", id, action);
        return toPlanVOWithEvents(plan);
    }

    @Transactional
    public PlanVO goOnline(Long id) {
        TrackerPlan plan = planMapper.selectById(id);
        if (plan == null) throw new RuntimeException("方案不存在");
        if (!"approved".equals(plan.getStatus()) && !"verified".equals(plan.getStatus())) {
            throw new RuntimeException("仅已通过或已验证状态可上线");
        }
        plan.setStatus("online");
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
        log.info("Plan {} went online", id);
        return toPlanVOWithEvents(plan);
    }

    // ==================== Helpers ====================

    private void saveEvents(Long planId, List<EventDef> events) {
        for (int i = 0; i < events.size(); i++) {
            EventDef def = events.get(i);
            TrackerPlanEvent evt = new TrackerPlanEvent();
            evt.setPlanId(planId);
            evt.setEventKey(def.getEventKey());
            evt.setEventName(def.getEventName());
            evt.setCategory(def.getCategory() != null ? def.getCategory() : "custom");
            evt.setDescription(def.getDescription());
            evt.setSpmCode(def.getSpmCode());
            evt.setSortOrder(i);
            evt.setCreatedAt(LocalDateTime.now());
            planEventMapper.insert(evt);

            if (def.getProperties() != null) {
                for (PropDef propDef : def.getProperties()) {
                    TrackerPlanProperty prop = new TrackerPlanProperty();
                    prop.setEventId(evt.getId());
                    prop.setPropKey(propDef.getPropKey());
                    prop.setPropName(propDef.getPropName());
                    prop.setDataType(propDef.getDataType() != null ? propDef.getDataType() : "string");
                    prop.setCreatedAt(LocalDateTime.now());
                    planPropertyMapper.insert(prop);
                }
            }
        }
    }

    private PlanVO toPlanVO(TrackerPlan p) {
        PlanVO vo = new PlanVO();
        vo.setId(p.getId());
        vo.setPlanName(p.getPlanName());
        vo.setAppId(p.getAppId());
        vo.setAppName(p.getAppName());
        vo.setAppVersion(p.getAppVersion());
        vo.setStatus(p.getStatus());
        vo.setSubmitter(p.getSubmitter());
        vo.setReviewer(p.getReviewer());
        vo.setReviewComment(p.getReviewComment());
        vo.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        vo.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return vo;
    }

    private PlanVO toPlanVOWithEvents(TrackerPlan p) {
        PlanVO vo = toPlanVO(p);
        LambdaQueryWrapper<TrackerPlanEvent> eqw = Wrappers.lambdaQuery();
        eqw.eq(TrackerPlanEvent::getPlanId, p.getId());
        eqw.orderByAsc(TrackerPlanEvent::getSortOrder);
        List<TrackerPlanEvent> evts = planEventMapper.selectList(eqw);

        List<EventVO> eventVOs = evts.stream().map(evt -> {
            EventVO evo = new EventVO();
            evo.setEventKey(evt.getEventKey());
            evo.setEventName(evt.getEventName());
            evo.setCategory(evt.getCategory());
            evo.setDescription(evt.getDescription());
            evo.setSpmCode(evt.getSpmCode());

            LambdaQueryWrapper<TrackerPlanProperty> pqw = Wrappers.lambdaQuery();
            pqw.eq(TrackerPlanProperty::getEventId, evt.getId());
            List<TrackerPlanProperty> props = planPropertyMapper.selectList(pqw);
            evo.setProperties(props.stream().map(prop -> {
                PropVO pvo = new PropVO();
                pvo.setPropKey(prop.getPropKey());
                pvo.setPropName(prop.getPropName());
                pvo.setDataType(prop.getDataType());
                return pvo;
            }).collect(Collectors.toList()));
            return evo;
        }).collect(Collectors.toList());

        vo.setEvents(eventVOs);
        return vo;
    }
}
