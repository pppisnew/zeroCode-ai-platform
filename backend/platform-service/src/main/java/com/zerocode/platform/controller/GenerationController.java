package com.zerocode.platform.controller;

import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.service.AiGenerationService;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.GenerationResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/generations")
public class GenerationController {

    private final AiGenerationService aiGenerationService;

    public GenerationController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @PostMapping("/html")
    public ApiResponse<GenerationResultVO> generateHtml(@Valid @RequestBody GenerateHtmlRequest request) {
        return ApiResponse.ok(aiGenerationService.generateHtml(request));
    }
}
