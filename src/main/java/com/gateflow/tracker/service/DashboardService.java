package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.CreateDashboardRequest;
import com.gateflow.tracker.domain.dto.DashboardVO;
import com.gateflow.tracker.domain.dto.UpdateDashboardRequest;
import com.gateflow.tracker.domain.entity.TrackerDashboard;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerDashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TrackerDashboardMapper dashboardMapper;

    public List<DashboardVO> listDashboards() {
        LambdaQueryWrapper<TrackerDashboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TrackerDashboard::getCreatedAt);
        return dashboardMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public DashboardVO getDashboardById(Long id) {
        TrackerDashboard dashboard = dashboardMapper.selectById(id);
        if (dashboard == null) {
            throw new BizException(ErrorCode.DASHBOARD_NOT_FOUND, "Dashboard not found: " + id);
        }
        return toVO(dashboard);
    }

    @Transactional(rollbackFor = Exception.class)
    public DashboardVO createDashboard(CreateDashboardRequest request) {
        TrackerDashboard dashboard = new TrackerDashboard();
        dashboard.setName(request.getName());
        dashboard.setConfig(request.getConfig());
        dashboard.setCreatedBy(request.getCreatedBy());
        dashboard.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        dashboardMapper.insert(dashboard);
        return toVO(dashboard);
    }

    @Transactional(rollbackFor = Exception.class)
    public DashboardVO updateDashboard(Long id, UpdateDashboardRequest request) {
        TrackerDashboard dashboard = dashboardMapper.selectById(id);
        if (dashboard == null) {
            throw new BizException(ErrorCode.DASHBOARD_NOT_FOUND, "Dashboard not found: " + id);
        }

        if (request.getName() != null) {
            dashboard.setName(request.getName());
        }
        if (request.getConfig() != null) {
            dashboard.setConfig(request.getConfig());
        }
        if (request.getStatus() != null) {
            dashboard.setStatus(request.getStatus());
        }

        dashboardMapper.updateById(dashboard);
        return toVO(dashboard);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDashboard(Long id) {
        if (dashboardMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.DASHBOARD_NOT_FOUND, "Dashboard not found: " + id);
        }
        dashboardMapper.deleteById(id);
    }

    private DashboardVO toVO(TrackerDashboard dashboard) {
        DashboardVO vo = new DashboardVO();
        vo.setId(dashboard.getId());
        vo.setName(dashboard.getName());
        vo.setConfig(dashboard.getConfig());
        vo.setCreatedBy(dashboard.getCreatedBy());
        vo.setStatus(dashboard.getStatus());
        vo.setCreatedAt(dashboard.getCreatedAt());
        vo.setUpdatedAt(dashboard.getUpdatedAt());
        return vo;
    }
}