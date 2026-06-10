package com.zerocode.platform.service;

import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.util.List;

public interface AppVersionService {

    AppVersionVO createVersion(Long appId, String prompt, GeneratedProjectVO project);

    AppVersionVO getVersion(Long appId, Integer versionNo);

    AppVersionVO getLatestVersion(Long appId);

    List<AppVersionVO> listVersions(Long appId);
}
