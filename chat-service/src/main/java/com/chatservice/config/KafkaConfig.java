package com.chatservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.message-sent}")
    private String messageSentTopic;

    @Value("${kafka.topic.message-delivered}")
    private String messageDeliveredTopic;

    @Value("${kafka.topic.message-read}")
    private String messageReadTopic;

    @Value("${kafka.topic.chat-archived}")
    private String chatArchivedTopic;

    @Value("${kafka.topic.group-event}")
    private String groupEventTopic;

    @Bean
    public NewTopic messageSentTopic() {
        return TopicBuilder.name(messageSentTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic messageDeliveredTopic() {
        return TopicBuilder.name(messageDeliveredTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic messageReadTopic() {
        return TopicBuilder.name(messageReadTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic chatArchivedTopic() {
        return TopicBuilder.name(chatArchivedTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic groupEventTopic() {
        return TopicBuilder.name(groupEventTopic).partitions(3).replicas(1).build();
    }
}
