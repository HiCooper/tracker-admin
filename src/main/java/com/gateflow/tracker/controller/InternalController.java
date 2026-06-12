package com.gateflow.tracker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.entity.TrackerSpm;
import com.gateflow.tracker.repository.TrackerSpmMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 内部 API — 供 tracker-service 查询 app 签名密钥。
 * 生产环境应通过网络策略限制仅允许 tracker-service IP 访问。
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final TrackerSpmMapper spmMapper;

    @GetMapping("/app-secret/{appCode}")
    public Map<String, String> getByAppCode(@PathVariable String appCode) {
        var spm = spmMapper.selectOne(new LambdaQueryWrapper<TrackerSpm>()
                .eq(TrackerSpm::getSpmCode, appCode).last("LIMIT 1"));
        return toMap(spm);
    }

    @GetMapping("/app-key/{appKey}")
    public Map<String, String> getByAppKey(@PathVariable String appKey) {
        var spm = spmMapper.selectOne(new LambdaQueryWrapper<TrackerSpm>()
                .eq(TrackerSpm::getAppKey, appKey).last("LIMIT 1"));
        return toMap(spm);
    }

    private Map<String, String> toMap(TrackerSpm spm) {
        if (spm == null || spm.getAppKey() == null) {
            return Map.of("appKey", "", "appSecret", "", "appCode", "");
        }
        return Map.of(
                "appKey", spm.getAppKey(),
                "appSecret", spm.getAppSecret() != null ? spm.getAppSecret() : "",
                "appCode", spm.getSpmCode() != null ? spm.getSpmCode() : ""
        );
    }
}
