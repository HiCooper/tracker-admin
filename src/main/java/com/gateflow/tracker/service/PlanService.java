package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.domain.entity.TrackerPlan;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final TrackerPlanMapper planMapper;
    private final SchemaPublishService schemaPublishService;
    private static final ObjectMapper om = new ObjectMapper();

    // ---- List ----
    public PageResponse<PlanVO> listPlans(Integer page, Integer size, String status) {
        Page<TrackerPlan> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 50);
        LambdaQueryWrapper<TrackerPlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(TrackerPlan::getStatus, status);
        }
        wrapper.orderByDesc(TrackerPlan::getCreatedAt);
        IPage<TrackerPlan> result = planMapper.selectPage(pageParam, wrapper);

        List<PlanVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageResponse.of(vos, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    // ---- Get ----
    public PlanVO getPlan(Long id) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        return toVO(p);
    }

    // ---- Create ----
    @Transactional(rollbackFor = Exception.class)
    public PlanVO createPlan(CreatePlanRequest req, String submitter) {
        TrackerPlan p = new TrackerPlan();
        p.setPlanName(req.getPlanName());
        p.setAppId(req.getAppId());
        p.setAppName(req.getAppName() != null ? req.getAppName() : "");
        p.setAppVersion(req.getAppVersion());
        p.setStatus("draft");
        p.setSubmitter(submitter != null ? submitter : "");
        p.setReviewer("");
        p.setReviewComment("");
        p.setEventsJson(toJson(req.getEvents()));
        planMapper.insert(p);
        log.info("Plan created: id={}, name={}", p.getId(), p.getPlanName());
        return toVO(p);
    }

    // ---- Update ----
    @Transactional(rollbackFor = Exception.class)
    public PlanVO updatePlan(Long id, UpdatePlanRequest req) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        if (!"draft".equals(p.getStatus()) && !"rejected".equals(p.getStatus())) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "Only draft/rejected plans can be edited");
        }
        if (req.getPlanName() != null) p.setPlanName(req.getPlanName());
        if (req.getAppVersion() != null) p.setAppVersion(req.getAppVersion());
        if (req.getEvents() != null) p.setEventsJson(toJson(req.getEvents()));
        planMapper.updateById(p);
        return toVO(p);
    }

    // ---- Delete ----
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        if (!"draft".equals(p.getStatus())) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "Only draft plans can be deleted");
        }
        planMapper.deleteById(id);
    }

    // ---- Submit for Review ----
    @Transactional(rollbackFor = Exception.class)
    public PlanVO submitForReview(Long id, String submitter) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        if (!"draft".equals(p.getStatus()) && !"rejected".equals(p.getStatus())) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "Only draft/rejected plans can be submitted");
        }
        if (submitter != null && !submitter.isBlank()) {
            p.setSubmitter(submitter);
        }
        p.setStatus("reviewing");
        planMapper.updateById(p);
        log.info("Plan submitted for review: id={}", id);
        return toVO(p);
    }

    // ---- Review ----
    @Transactional(rollbackFor = Exception.class)
    public PlanVO reviewPlan(Long id, ReviewPlanRequest req, String reviewer) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        if (!"reviewing".equals(p.getStatus())) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "Only reviewing plans can be reviewed");
        }
        if ("approve".equals(req.getAction())) {
            p.setStatus("approved");
        } else if ("reject".equals(req.getAction())) {
            p.setStatus("rejected");
        } else {
            throw new BizException(ErrorCode.PARAM_INVALID, "Action must be 'approve' or 'reject'");
        }
        p.setReviewer(reviewer != null ? reviewer : "");
        p.setReviewComment(req.getComment() != null ? req.getComment() : "");
        planMapper.updateById(p);
        log.info("Plan reviewed: id={}, action={}", id, req.getAction());
        return toVO(p);
    }

    // ---- Go Online ----
    @Transactional(rollbackFor = Exception.class)
    public PlanVO goOnline(Long id) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        if (!"approved".equals(p.getStatus()) && !"verified".equals(p.getStatus())) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "Only approved/verified plans can go online");
        }
        p.setStatus("online");
        planMapper.updateById(p);
        // 上线即发布事件契约到 Redis(best-effort,失败不影响上线)
        schemaPublishService.publishForPlan(p);
        log.info("Plan online: id={}", id);
        return toVO(p);
    }

    /** 手动(重新)发布某方案的事件契约;返回发布所用 appCode(失败 null)。 */
    public String publishSchema(Long id) {
        TrackerPlan p = planMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id);
        return schemaPublishService.publishForPlan(p);
    }

    // ---- Helpers ----
    private PlanVO toVO(TrackerPlan p) {
        PlanVO vo = new PlanVO();
        vo.setId(p.getId());
        vo.setPlanName(p.getPlanName());
        vo.setAppId(p.getAppId());
        vo.setAppName(p.getAppName());
        vo.setAppVersion(p.getAppVersion());
        vo.setStatus(p.getStatus());
        vo.setEvents(parseEvents(p.getEventsJson()));
        vo.setSubmitter(p.getSubmitter());
        vo.setReviewer(p.getReviewer());
        vo.setReviewComment(p.getReviewComment());
        vo.setCreatedAt(p.getCreatedAt());
        vo.setUpdatedAt(p.getUpdatedAt());
        return vo;
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try { return om.writeValueAsString(obj); } catch (JsonProcessingException e) { return "[]"; }
    }

    private List<PlanVO.EventItem> parseEvents(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return om.readValue(json, new TypeReference<List<PlanVO.EventItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse events_json: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
