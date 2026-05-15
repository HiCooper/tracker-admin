package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.SessionAnalysisVO;
import com.gateflow.tracker.domain.entity.TrackerSessionAgg;
import com.gateflow.tracker.repository.TrackerSessionAggMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAnalysisService {

    private final TrackerSessionAggMapper sessionAggMapper;

    public List<SessionAnalysisVO> query(String sessionId, String userId, LocalDate startDate, LocalDate endDate) {
        // Note: sessionId and userId are for future detailed session lookup
        // Currently we aggregate by date/hour from tracker_session_agg
        
        LambdaQueryWrapper<TrackerSessionAgg> wrapper = new LambdaQueryWrapper<>();
        
        // 日期范围过滤
        if (startDate != null) {
            wrapper.ge(TrackerSessionAgg::getDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(TrackerSessionAgg::getDate, endDate);
        }
        
        // 按日期和小时降序排序
        wrapper.orderByDesc(TrackerSessionAgg::getDate)
               .orderByDesc(TrackerSessionAgg::getHour);
        
        // 限制返回数量
        wrapper.last("LIMIT 1000");
        
        List<TrackerSessionAgg> results = sessionAggMapper.selectList(wrapper);
        
        return results.stream().map(this::toVO).collect(Collectors.toList());
    }

    public List<SessionAnalysisVO> getRecentData(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        LambdaQueryWrapper<TrackerSessionAgg> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(TrackerSessionAgg::getDate, startDate);
        wrapper.le(TrackerSessionAgg::getDate, endDate);
        wrapper.orderByDesc(TrackerSessionAgg::getDate);
        wrapper.last("LIMIT 100");
        
        List<TrackerSessionAgg> results = sessionAggMapper.selectList(wrapper);
        return results.stream().map(this::toVO).collect(Collectors.toList());
    }

    private SessionAnalysisVO toVO(TrackerSessionAgg agg) {
        SessionAnalysisVO vo = new SessionAnalysisVO();
        vo.setDate(agg.getDate());
        vo.setHour(agg.getHour());
        vo.setPlatform(agg.getPlatform());
        vo.setSessionCount(agg.getSessionCount());
        vo.setUserCount(agg.getUserCount());
        vo.setAvgDuration(agg.getAvgDuration());
        vo.setAvgPageDepth(agg.getAvgPageDepth());
        vo.setBounceCount(agg.getBounceCount());
        vo.setBounceRate(agg.getBounceRate());
        return vo;
    }
}