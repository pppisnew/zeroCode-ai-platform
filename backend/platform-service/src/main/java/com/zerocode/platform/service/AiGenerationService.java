package com.zerocode.platform.service;

import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.vo.GenerationResultVO;

public interface AiGenerationService {

    GenerationResultVO generateHtml(GenerateHtmlRequest request);
}
