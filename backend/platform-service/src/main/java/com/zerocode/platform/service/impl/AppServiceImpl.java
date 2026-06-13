package com.zerocode.platform.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zerocode.platform.mapper.AppMapper;
import com.zerocode.platform.mapper.AppVersionMapper;
import com.zerocode.platform.model.AppEntity;
import com.zerocode.platform.model.AppVersionEntity;
import com.zerocode.platform.service.AppService;
import com.zerocode.platform.vo.AppVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppServiceImpl implements AppService {

    private static final int MAX_LIST_SIZE = 100;

    private final AppMapper appMapper;
    private final AppVersionMapper appVersionMapper;
    private final long defaultUserId;

    public AppServiceImpl(
            AppMapper appMapper,
            AppVersionMapper appVersionMapper,
            @Value("${zerocode.default-user-id:1}") long defaultUserId) {
        this.appMapper = appMapper;
        this.appVersionMapper = appVersionMapper;
        this.defaultUserId = defaultUserId;
    }

    @Override
    public List<AppVO> listApps() {
        long userId = resolveUserId();
        return appMapper.selectList(new LambdaQueryWrapper<AppEntity>()
                        .eq(AppEntity::getUserId, userId)
                        .orderByDesc(AppEntity::getCreateTime)
                        .last("LIMIT " + MAX_LIST_SIZE))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public AppVO createApp(com.zerocode.platform.dto.CreateAppRequest request) {
        return createGeneratedApp(request.appName(), request.description(), request.type());
    }

    @Override
    @Transactional
    public AppVO createGeneratedApp(String appName, String description, String type) {
        AppEntity app = new AppEntity();
        app.setUserId(defaultUserId);
        app.setAppName(appName);
        app.setDescription(description);
        app.setType(type);
        app.setStatus("draft");
        app.setDeployUrl(null);
        app.setCreateTime(LocalDateTime.now());
        appMapper.insert(app);
        return toVO(app);
    }

    @Override
    @Cacheable(value = "apps", key = "#id")
    public AppVO getApp(Long id) {
        AppEntity app = appMapper.selectById(id);
        if (app == null) {
            throw new IllegalArgumentException("App not found");
        }
        long userId = resolveUserId();
        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("App not found");
        }
        return toVO(app);
    }

    @Override
    @Transactional
    public void deleteApp(Long id) {
        AppEntity app = appMapper.selectById(id);
        if (app == null) {
            throw new IllegalArgumentException("App not found");
        }
        long userId = resolveUserId();
        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("App not found");
        }
        appVersionMapper.delete(new LambdaQueryWrapper<AppVersionEntity>()
                .eq(AppVersionEntity::getAppId, id));
        appMapper.deleteById(id);
    }

    private long resolveUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            return defaultUserId;
        }
    }

    private AppVO toVO(AppEntity app) {
        return new AppVO(
                app.getId(),
                app.getUserId(),
                app.getAppName(),
                app.getDescription(),
                app.getType(),
                app.getStatus(),
                app.getDeployUrl(),
                app.getCreateTime());
    }
}
