package com.sidequest.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic postTopic() {
        return TopicBuilder.name("post-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name("user-events")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic videoProcessTopic() {
        return TopicBuilder.name("video-process-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatMessageTopic() {
        return TopicBuilder.name("chat-message-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

