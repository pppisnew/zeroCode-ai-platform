package com.zerocode.platform.controller;

import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.mq.AiGenerationListener.GenerationTask;
import com.zerocode.platform.service.AiGenerationService;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.GenerationResultVO;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.zerocode.platform.config.RabbitConfig.AI_GENERATION_ROUTING_KEY;
import static com.zerocode.platform.config.RabbitConfig.AI_GENERATION_EXCHANGE;

@RestController
@RequestMapping("/generations")
public class GenerationController {

    private final AiGenerationService aiGenerationService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    public GenerationController(
            AiGenerationService aiGenerationService,
            RabbitTemplate rabbitTemplate,
            StringRedisTemplate redisTemplate) {
        this.aiGenerationService = aiGenerationService;
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/html")
    public ApiResponse<GenerationResultVO> generateHtml(@Valid @RequestBody GenerateHtmlRequest request) {
        return ApiResponse.ok(aiGenerationService.generateHtml(request));
    }

    @PostMapping("/vue")
    public ApiResponse<GenerationResultVO> generateVue(@Valid @RequestBody GenerateHtmlRequest request) {
        return ApiResponse.ok(aiGenerationService.generateHtml(request));
    }

    @PostMapping("/react")
    public ApiResponse<GenerationResultVO> generateReact(@Valid @RequestBody GenerateHtmlRequest request) {
        return ApiResponse.ok(aiGenerationService.generateHtml(request));
    }

    @PostMapping("/async")
    public ApiResponse<Map<String, String>> generateAsync(@Valid @RequestBody GenerateHtmlRequest request) {
        GenerationTask task = GenerationTask.create(
                request.prompt(), request.appId(), request.normalizedProjectType());
        rabbitTemplate.convertAndSend(AI_GENERATION_EXCHANGE, AI_GENERATION_ROUTING_KEY, task);
        redisTemplate.opsForValue().set("gen:" + task.taskId() + ":status", "queued", 30, TimeUnit.MINUTES);
        return ApiResponse.ok(Map.of("taskId", task.taskId(), "status", "queued"));
    }

    @GetMapping("/async/{taskId}")
    public ApiResponse<Map<String, String>> getAsyncStatus(@PathVariable String taskId) {
        String status = redisTemplate.opsForValue().get("gen:" + taskId + ":status");
        if (status == null) {
            return ApiResponse.fail(404, "Task not found");
        }
        Map<String, String> response = new HashMap<>(Map.of("taskId", taskId, "status", status));
        if ("completed".equals(status)) {
            response.put("result", redisTemplate.opsForValue().get("gen:" + taskId + ":result"));
        } else if ("failed".equals(status)) {
            response.put("error", redisTemplate.opsForValue().get("gen:" + taskId + ":error"));
        }
        return ApiResponse.ok(response);
    }
}
