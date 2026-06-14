package com.gateflow.tracker.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gateflow.tracker.domain.entity.TrackerFunction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackerFunctionMapper extends BaseMapper<TrackerFunction> {
}
