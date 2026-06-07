package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.entity.TrackerSdkConfig;
import com.gateflow.tracker.repository.TrackerSdkConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SdkConfigService {

    private final TrackerSdkConfigMapper configMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class ConfigRequest {
        private String configName;
        private String appId;
        private String appVersion;
        private String platform;
        private Map<String, Object> configData;
        private Integer priority;
    }

    public List<TrackerSdkConfig> list() {
        return configMapper.selectList(Wrappers.lambdaQuery(TrackerSdkConfig.class)
                .orderByDesc(TrackerSdkConfig::getUpdatedAt));
    }

    public TrackerSdkConfig get(Long id) { return configMapper.selectById(id); }

    @Transactional
    public TrackerSdkConfig create(ConfigRequest req) {
        TrackerSdkConfig c = new TrackerSdkConfig();
        c.setConfigName(req.getConfigName());
        c.setAppId(req.getAppId());
        c.setAppVersion(req.getAppVersion() != null ? req.getAppVersion() : "*");
        c.setPlatform(req.getPlatform() != null ? req.getPlatform() : "*");
        try { c.setConfigData(objectMapper.writeValueAsString(req.getConfigData())); } catch (Exception ignored) {}
        c.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        c.setStatus(1);
        configMapper.insert(c);
        return c;
    }

    @Transactional
    public TrackerSdkConfig update(Long id, ConfigRequest req) {
        TrackerSdkConfig c = configMapper.selectById(id);
        if (c == null) throw new RuntimeException("配置不存在");
        if (req.getConfigName() != null) c.setConfigName(req.getConfigName());
        if (req.getAppId() != null) c.setAppId(req.getAppId());
        if (req.getAppVersion() != null) c.setAppVersion(req.getAppVersion());
        if (req.getPlatform() != null) c.setPlatform(req.getPlatform());
        if (req.getConfigData() != null) {
            try { c.setConfigData(objectMapper.writeValueAsString(req.getConfigData())); } catch (Exception ignored) {}
        }
        if (req.getPriority() != null) c.setPriority(req.getPriority());
        configMapper.updateById(c);
        return c;
    }

    @Transactional
    public void delete(Long id) { configMapper.deleteById(id); }

    /** SDK pull: merge all matching configs by priority. */
    public Map<String, Object> pullConfig(String appId, String platform, String appVersion) {
        var qw = Wrappers.lambdaQuery(TrackerSdkConfig.class)
                .eq(TrackerSdkConfig::getAppId, appId)
                .eq(TrackerSdkConfig::getStatus, 1)
                .and(w -> w.eq(TrackerSdkConfig::getPlatform, platform).or().eq(TrackerSdkConfig::getPlatform, "*"))
                .and(w -> w.eq(TrackerSdkConfig::getAppVersion, appVersion).or().eq(TrackerSdkConfig::getAppVersion, "*"))
                .orderByDesc(TrackerSdkConfig::getPriority);
        List<TrackerSdkConfig> configs = configMapper.selectList(qw);

        Map<String, Object> merged = new LinkedHashMap<>();
        if (configs.isEmpty()) {
            merged.put("samplingRate", 1.0);
            merged.put("batchSize", 10);
            merged.put("flushIntervalMs", 5000);
        } else {
            for (int i = configs.size() - 1; i >= 0; i--) {
                try {
                    Map<String, Object> data = objectMapper.readValue(configs.get(i).getConfigData(), new TypeReference<>() {});
                    merged.putAll(data);
                } catch (Exception ignored) {}
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appId", appId);
        result.put("platform", platform);
        result.put("appVersion", appVersion);
        result.put("config", merged);
        result.put("configCount", configs.size());
        return result;
    }
}
