package com.gateflow.tracker.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gateflow.tracker.domain.entity.TrackerSegment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackerSegmentMapper extends BaseMapper<TrackerSegment> {
}
