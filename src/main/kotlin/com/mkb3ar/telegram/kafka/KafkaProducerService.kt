package com.mkb3ar.telegram.kafka

import com.mkb3ar.telegram.dto.UserDataDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(
    private val kafkaTemplate: KafkaTemplate<String, UserDataDTO>
) {
    @Value("\${kafka.topic.process}")
    private lateinit var topicName: String

    fun sendFileToProcess(request: UserDataDTO){
        kafkaTemplate.send(topicName, request)
    }
}