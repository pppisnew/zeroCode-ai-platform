package com.zerocode.platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String AI_GENERATION_QUEUE = "ai.generation.queue";
    public static final String AI_GENERATION_EXCHANGE = "ai.generation.exchange";
    public static final String AI_GENERATION_ROUTING_KEY = "ai.generation";

    @Bean
    public Queue aiGenerationQueue() {
        return new Queue(AI_GENERATION_QUEUE, true);
    }

    @Bean
    public DirectExchange aiGenerationExchange() {
        return new DirectExchange(AI_GENERATION_EXCHANGE, true, false);
    }

    @Bean
    public Binding aiGenerationBinding() {
        return BindingBuilder.bind(aiGenerationQueue())
                .to(aiGenerationExchange())
                .with(AI_GENERATION_ROUTING_KEY);
    }
}
