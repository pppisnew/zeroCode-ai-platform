package com.zerocode.platform.mq;

import com.zerocode.platform.config.RabbitConfig;
import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.service.AiGenerationService;
import com.zerocode.platform.vo.GenerationResultVO;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiGenerationListener {

    private final AiGenerationService aiGenerationService;
    private final StringRedisTemplate redis;

    public AiGenerationListener(AiGenerationService aiGenerationService, StringRedisTemplate redis) {
        this.aiGenerationService = aiGenerationService;
        this.redis = redis;
    }

    @RabbitListener(queues = RabbitConfig.AI_GENERATION_QUEUE)
    public void onGenerationRequest(GenerationTask task) {
        String taskId = task.taskId();
        try {
            redis.opsForValue().set("gen:" + taskId + ":status", "processing", 30, TimeUnit.MINUTES);
            GenerationResultVO result = aiGenerationService.generateHtml(
                    new GenerateHtmlRequest(task.prompt(), task.appId(), task.projectType()));
            redis.opsForValue().set("gen:" + taskId + ":result",
                    toResultJson(result), 30, TimeUnit.MINUTES);
            redis.opsForValue().set("gen:" + taskId + ":status", "completed", 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            redis.opsForValue().set("gen:" + taskId + ":status", "failed", 30, TimeUnit.MINUTES);
            redis.opsForValue().set("gen:" + taskId + ":error",
                    e.getMessage() != null ? e.getMessage() : "Unknown error", 30, TimeUnit.MINUTES);
        }
    }

    private String toResultJson(GenerationResultVO result) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }

    public record GenerationTask(String taskId, String prompt, Long appId, String projectType) {
        public static GenerationTask create(String prompt, Long appId, String projectType) {
            return new GenerationTask(UUID.randomUUID().toString(), prompt, appId, projectType);
        }
    }
}
