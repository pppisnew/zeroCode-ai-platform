package com.zerocode.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zerocode.platform.dto.CreateAppRequest;
import com.zerocode.platform.mapper.AppMapper;
import com.zerocode.platform.model.AppEntity;
import com.zerocode.platform.service.AppService;
import com.zerocode.platform.vo.AppVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AppServiceImpl implements AppService {

    private final AppMapper appMapper;

    public AppServiceImpl(AppMapper appMapper) {
        this.appMapper = appMapper;
    }

    @Override
    public List<AppVO> listApps() {
        return appMapper.selectList(new LambdaQueryWrapper<AppEntity>()
                        .orderByDesc(AppEntity::getCreateTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public AppVO createApp(CreateAppRequest request) {
        return createGeneratedApp(request.appName(), request.description(), request.type());
    }

    @Override
    public AppVO createGeneratedApp(String appName, String description, String type) {
        AppEntity app = new AppEntity();
        app.setUserId(1L);
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
    public AppVO getApp(Long id) {
        AppEntity app = appMapper.selectById(id);
        if (app == null) {
            throw new IllegalArgumentException("App not found");
        }
        return toVO(app);
    }

    @Override
    public void deleteApp(Long id) {
        appMapper.deleteById(id);
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
