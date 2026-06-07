package com.gateflow.tracker.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gateflow.tracker.domain.entity.TrackerPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackerPlanMapper extends BaseMapper<TrackerPlan> {
}
