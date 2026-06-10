package com.zerocode.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.mapper.AppVersionMapper;
import com.zerocode.platform.model.AppVersionEntity;
import com.zerocode.platform.service.AppVersionService;
import com.zerocode.platform.util.ProjectFileValidator;
import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AppVersionServiceImpl implements AppVersionService {

    private final AppVersionMapper appVersionMapper;
    private final ObjectMapper objectMapper;

    public AppVersionServiceImpl(AppVersionMapper appVersionMapper, ObjectMapper objectMapper) {
        this.appVersionMapper = appVersionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public AppVersionVO createVersion(Long appId, String prompt, GeneratedProjectVO project) {
        ProjectFileValidator.validateProject(project);
        AppVersionEntity latest = appVersionMapper.selectOne(new LambdaQueryWrapper<AppVersionEntity>()
                .eq(AppVersionEntity::getAppId, appId)
                .orderByDesc(AppVersionEntity::getVersionNo)
                .last("LIMIT 1"));
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;

        AppVersionEntity version = new AppVersionEntity();
        version.setAppId(appId);
        version.setVersionNo(versionNo);
        version.setPrompt(prompt);
        version.setAiResponse(writeProject(project));
        version.setSnapshotUrl(null);
        version.setCreateTime(LocalDateTime.now());
        appVersionMapper.insert(version);
        return toVO(version);
    }

    @Override
    public AppVersionVO getVersion(Long appId, Integer versionNo) {
        AppVersionEntity version = appVersionMapper.selectOne(new LambdaQueryWrapper<AppVersionEntity>()
                .eq(AppVersionEntity::getAppId, appId)
                .eq(AppVersionEntity::getVersionNo, versionNo)
                .last("LIMIT 1"));
        if (version == null) {
            throw new IllegalArgumentException("Version not found");
        }
        return toVO(version);
    }

    @Override
    public AppVersionVO getLatestVersion(Long appId) {
        AppVersionEntity version = appVersionMapper.selectOne(new LambdaQueryWrapper<AppVersionEntity>()
                .eq(AppVersionEntity::getAppId, appId)
                .orderByDesc(AppVersionEntity::getVersionNo)
                .last("LIMIT 1"));
        if (version == null) {
            return null;
        }
        return toVO(version);
    }

    @Override
    public List<AppVersionVO> listVersions(Long appId) {
        return appVersionMapper.selectList(new LambdaQueryWrapper<AppVersionEntity>()
                        .eq(AppVersionEntity::getAppId, appId)
                        .orderByAsc(AppVersionEntity::getVersionNo))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private AppVersionVO toVO(AppVersionEntity version) {
        return new AppVersionVO(
                version.getId(),
                version.getAppId(),
                version.getVersionNo(),
                version.getPrompt(),
                readProject(version.getAiResponse()),
                version.getCreateTime());
    }

    private String writeProject(GeneratedProjectVO project) {
        try {
            return objectMapper.writeValueAsString(project);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize generated project");
        }
    }

    private GeneratedProjectVO readProject(String aiResponse) {
        try {
            return objectMapper.readValue(aiResponse, GeneratedProjectVO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to deserialize generated project");
        }
    }
}
