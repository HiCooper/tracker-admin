package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.domain.entity.TrackerEvent;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final TrackerEventMapper eventMapper;

    public PageResponse<EventVO> listEvents(Integer page, Integer size, String sort, String order) {
        Page<TrackerEvent> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<TrackerEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderBy(StringUtils.hasText(sort),
                "asc".equalsIgnoreCase(order), TrackerEvent::getCreatedAt);

        IPage<TrackerEvent> pageResult = eventMapper.selectPage(pageParam, wrapper);

        List<EventVO> voList = pageResult.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResponse.of(voList, pageResult.getTotal(), page, size);
    }

    public EventVO getEventById(Long id) {
        TrackerEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "Event not found: " + id);
        }
        return toVO(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public EventVO createEvent(CreateEventRequest request) {
        LambdaQueryWrapper<TrackerEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackerEvent::getEventKey, request.getEventKey());
        if (eventMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.EVENT_KEY_DUPLICATED, "Event key already exists: " + request.getEventKey());
        }

        TrackerEvent event = new TrackerEvent();
        event.setEventKey(request.getEventKey());
        event.setEventName(request.getEventName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory() != null ? request.getCategory() : "custom");
        event.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        eventMapper.insert(event);
        return toVO(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public EventVO updateEvent(Long id, UpdateEventRequest request) {
        TrackerEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "Event not found: " + id);
        }

        if (request.getEventName() != null) {
            event.setEventName(request.getEventName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        eventMapper.updateById(event);
        return toVO(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long id) {
        if (eventMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "Event not found: " + id);
        }
        eventMapper.deleteById(id);
    }

    private EventVO toVO(TrackerEvent event) {
        EventVO vo = new EventVO();
        vo.setId(event.getId());
        vo.setEventKey(event.getEventKey());
        vo.setEventName(event.getEventName());
        vo.setDescription(event.getDescription());
        vo.setCategory(event.getCategory());
        vo.setStatus(event.getStatus());
        vo.setCreatedAt(event.getCreatedAt());
        vo.setUpdatedAt(event.getUpdatedAt());
        return vo;
    }
}