package com.gateflow.tracker.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gateflow.tracker.domain.entity.TrackerDashboard;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackerDashboardMapper extends BaseMapper<TrackerDashboard> {
}
