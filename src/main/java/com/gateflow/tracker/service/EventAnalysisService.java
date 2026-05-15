package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.EventAnalysisQueryRequest;
import com.gateflow.tracker.domain.dto.EventAnalysisVO;
import com.gateflow.tracker.domain.entity.TrackerEventAgg;
import com.gateflow.tracker.repository.TrackerEventAggMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAnalysisService {

    private final TrackerEventAggMapper eventAggMapper;

    public List<EventAnalysisVO> query(EventAnalysisQueryRequest request) {
        LambdaQueryWrapper<TrackerEventAgg> wrapper = new LambdaQueryWrapper<>();
        
        // 日期范围过滤
        if (request.getStartDate() != null) {
            wrapper.ge(TrackerEventAgg::getDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(TrackerEventAgg::getDate, request.getEndDate());
        }
        
        // 事件类型过滤 (eventKey -> eventType)
        if (request.getEventKey() != null && !request.getEventKey().isEmpty()) {
            wrapper.like(TrackerEventAgg::getEventType, request.getEventKey());
        }
        
        // 平台过滤
        if (request.getPlatform() != null && !request.getPlatform().isEmpty()) {
            wrapper.eq(TrackerEventAgg::getPlatform, request.getPlatform());
        }
        
        // 按日期和小时降序排序
        wrapper.orderByDesc(TrackerEventAgg::getDate)
               .orderByDesc(TrackerEventAgg::getHour);
        
        // 限制返回数量
        wrapper.last("LIMIT 1000");
        
        List<TrackerEventAgg> results = eventAggMapper.selectList(wrapper);
        
        return results.stream().map(this::toVO).collect(Collectors.toList());
    }

    public List<EventAnalysisVO> getRecentData(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        LambdaQueryWrapper<TrackerEventAgg> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(TrackerEventAgg::getDate, startDate);
        wrapper.le(TrackerEventAgg::getDate, endDate);
        wrapper.orderByDesc(TrackerEventAgg::getDate);
        wrapper.last("LIMIT 100");
        
        List<TrackerEventAgg> results = eventAggMapper.selectList(wrapper);
        return results.stream().map(this::toVO).collect(Collectors.toList());
    }

    private EventAnalysisVO toVO(TrackerEventAgg agg) {
        EventAnalysisVO vo = new EventAnalysisVO();
        vo.setDate(agg.getDate());
        vo.setHour(agg.getHour());
        vo.setPlatform(agg.getPlatform());
        vo.setEventType(agg.getEventType());
        vo.setEventCount(agg.getEventCount());
        vo.setUserCount(agg.getUserCount());
        vo.setDeviceCount(agg.getDeviceCount());
        return vo;
    }
}