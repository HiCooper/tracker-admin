package com.gateflow.tracker.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackerAlertEventMapper extends BaseMapper<TrackerAlertEvent> {
}
