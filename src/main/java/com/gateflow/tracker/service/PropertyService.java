package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.CreatePropertyRequest;
import com.gateflow.tracker.domain.dto.EventVO;
import com.gateflow.tracker.domain.dto.PropertyVO;
import com.gateflow.tracker.domain.entity.TrackerEvent;
import com.gateflow.tracker.domain.entity.TrackerProperty;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerEventMapper;
import com.gateflow.tracker.repository.TrackerPropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final TrackerPropertyMapper propertyMapper;
    private final TrackerEventMapper eventMapper;

    public List<PropertyVO> getPropertiesByEventId(Long eventId) {
        if (eventMapper.selectById(eventId) == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "Event not found: " + eventId);
        }

        LambdaQueryWrapper<TrackerProperty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackerProperty::getEventId, eventId);

        List<TrackerProperty> properties = propertyMapper.selectList(wrapper);
        return properties.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public PropertyVO createProperty(CreatePropertyRequest request) {
        TrackerEvent event = eventMapper.selectById(request.getEventId());
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "Event not found: " + request.getEventId());
        }

        LambdaQueryWrapper<TrackerProperty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackerProperty::getEventId, request.getEventId())
                .eq(TrackerProperty::getPropKey, request.getPropKey());
        if (propertyMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "Property key already exists for this event");
        }

        TrackerProperty property = new TrackerProperty();
        property.setEventId(request.getEventId());
        property.setPropKey(request.getPropKey());
        property.setPropName(request.getPropName());
        property.setDataType(request.getDataType() != null ? request.getDataType() : "string");
        property.setDescription(request.getDescription());

        propertyMapper.insert(property);
        return toVO(property, event.getEventName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProperty(Long id) {
        if (propertyMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.PROPERTY_NOT_FOUND, "Property not found: " + id);
        }
        propertyMapper.deleteById(id);
    }

    private PropertyVO toVO(TrackerProperty property) {
        TrackerEvent event = eventMapper.selectById(property.getEventId());
        String eventName = event != null ? event.getEventName() : "";
        return toVO(property, eventName);
    }

    private PropertyVO toVO(TrackerProperty property, String eventName) {
        PropertyVO vo = new PropertyVO();
        vo.setId(property.getId());
        vo.setEventId(property.getEventId());
        vo.setEventName(eventName);
        vo.setPropKey(property.getPropKey());
        vo.setPropName(property.getPropName());
        vo.setDataType(property.getDataType());
        vo.setDescription(property.getDescription());
        vo.setCreatedAt(property.getCreatedAt());
        return vo;
    }
}