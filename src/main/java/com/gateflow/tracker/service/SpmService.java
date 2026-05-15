package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.CreateSpmRequest;
import com.gateflow.tracker.domain.dto.SpmVO;
import com.gateflow.tracker.domain.entity.TrackerSpm;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerSpmMapper;
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
public class SpmService {

    private final TrackerSpmMapper spmMapper;

    public List<SpmVO> listSpms() {
        LambdaQueryWrapper<TrackerSpm> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TrackerSpm::getCreatedAt);
        return spmMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public SpmVO getSpmById(Long id) {
        TrackerSpm spm = spmMapper.selectById(id);
        if (spm == null) {
            throw new BizException(ErrorCode.SPM_NOT_FOUND, "SPM not found: " + id);
        }
        return toVO(spm);
    }

    @Transactional(rollbackFor = Exception.class)
    public SpmVO createSpm(CreateSpmRequest request) {
        LambdaQueryWrapper<TrackerSpm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackerSpm::getSpmCode, request.getSpmCode());
        if (spmMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.SPM_CODE_DUPLICATED, "SPM code already exists: " + request.getSpmCode());
        }

        TrackerSpm spm = new TrackerSpm();
        spm.setSpmCode(request.getSpmCode());
        spm.setSpmName(request.getSpmName());
        spm.setSpmaLabel(request.getSpmaLabel());
        spm.setSpmbLabel(request.getSpmbLabel());
        spm.setSpmcLabel(request.getSpmcLabel());
        spm.setSpmdLabel(request.getSpmdLabel());
        spm.setDescription(request.getDescription());
        spm.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        spmMapper.insert(spm);
        return toVO(spm);
    }

    @Transactional(rollbackFor = Exception.class)
    public SpmVO updateSpm(Long id, CreateSpmRequest request) {
        TrackerSpm spm = spmMapper.selectById(id);
        if (spm == null) {
            throw new BizException(ErrorCode.SPM_NOT_FOUND, "SPM not found: " + id);
        }

        if (request.getSpmName() != null) {
            spm.setSpmName(request.getSpmName());
        }
        if (request.getSpmaLabel() != null) {
            spm.setSpmaLabel(request.getSpmaLabel());
        }
        if (request.getSpmbLabel() != null) {
            spm.setSpmbLabel(request.getSpmbLabel());
        }
        if (request.getSpmcLabel() != null) {
            spm.setSpmcLabel(request.getSpmcLabel());
        }
        if (request.getSpmdLabel() != null) {
            spm.setSpmdLabel(request.getSpmdLabel());
        }
        if (request.getDescription() != null) {
            spm.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            spm.setStatus(request.getStatus());
        }

        spmMapper.updateById(spm);
        return toVO(spm);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSpm(Long id) {
        if (spmMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.SPM_NOT_FOUND, "SPM not found: " + id);
        }
        spmMapper.deleteById(id);
    }

    private SpmVO toVO(TrackerSpm spm) {
        SpmVO vo = new SpmVO();
        vo.setId(spm.getId());
        vo.setSpmCode(spm.getSpmCode());
        vo.setSpmName(spm.getSpmName());
        vo.setSpmaLabel(spm.getSpmaLabel());
        vo.setSpmbLabel(spm.getSpmbLabel());
        vo.setSpmcLabel(spm.getSpmcLabel());
        vo.setSpmdLabel(spm.getSpmdLabel());
        vo.setDescription(spm.getDescription());
        vo.setStatus(spm.getStatus());
        vo.setCreatedAt(spm.getCreatedAt());
        vo.setUpdatedAt(spm.getUpdatedAt());
        return vo;
    }
}