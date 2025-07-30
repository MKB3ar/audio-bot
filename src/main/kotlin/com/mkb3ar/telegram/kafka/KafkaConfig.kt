package com.mkb3ar.telegram.kafka

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    @Value("\${kafka.topic.process}")
    private lateinit var processTopicName: String

    @Value("\${kafka.topic.done}")
    private lateinit var doneTopicName: String // <-- Добавляем переменную для нового топика

    @Bean
    fun topic1() =
        TopicBuilder.name(processTopicName)
            .partitions(10)
            .replicas(3)
            .compact()
            .build()

    @Bean
    fun topic2() =
        TopicBuilder.name(doneTopicName)
            .partitions(10)
            .replicas(3)
            .compact()
            .build()
}