package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gateflow.tracker.domain.dto.PageResponse;
import com.gateflow.tracker.domain.entity.TrackerAuditLog;
import com.gateflow.tracker.repository.TrackerAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final TrackerAuditLogMapper auditLogMapper;

    /** 记录一条审计日志。审计失败绝不影响主流程。 */
    public void record(TrackerAuditLog entry) {
        try {
            auditLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("Failed to persist audit log for {} {}: {}",
                    entry.getMethod(), entry.getPath(), e.getMessage());
        }
    }

    /** 分页查询审计日志,按时间倒序;可选按用户名过滤。 */
    public PageResponse<TrackerAuditLog> query(Integer page, Integer size, String username) {
        Page<TrackerAuditLog> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 50);
        LambdaQueryWrapper<TrackerAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.eq(TrackerAuditLog::getUsername, username);
        }
        wrapper.orderByDesc(TrackerAuditLog::getCreatedAt);
        IPage<TrackerAuditLog> result = auditLogMapper.selectPage(pageParam, wrapper);
        return PageResponse.of(result.getRecords(), result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }
}
