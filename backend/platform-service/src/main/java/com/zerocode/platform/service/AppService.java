package com.zerocode.platform.service;

import com.zerocode.platform.dto.CreateAppRequest;
import com.zerocode.platform.vo.AppVO;
import java.util.List;

public interface AppService {

    List<AppVO> listApps();

    AppVO createApp(CreateAppRequest request);

    AppVO createGeneratedApp(String appName, String description, String type);

    AppVO getApp(Long id);

    void deleteApp(Long id);
}
