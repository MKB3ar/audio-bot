package com.mkb3ar.telegram.kafka

import com.mkb3ar.telegram.dto.DoneUserdataDTO
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.io.File

@Service
class KafkaConsumerService(
    private val telegramClient: TelegramClient
) {
    @KafkaListener(
        topics = ["\${kafka.topic.done}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun listenDoneTopic(result: DoneUserdataDTO) {
        try {
            println(result)
            if (result.status == "success" && result.filePath != null) {
                // Если обработка прошла успешно, отправляем текстовый файл
                val textFile = File(result.filePath)

                if (textFile.exists()) {
                    val sendDocument = SendDocument.builder()
                        .chatId(result.chatId.toString())
                        .document(InputFile(textFile, "${result.userFileName}.txt"))
                        .caption("✅ Вот текст из вашего файла '${result.userFileName}'.")
                        .build()
                    telegramClient.execute(sendDocument)
                } else {
                    sendReply(result.chatId, "❌ Произошла ошибка: не удалось найти итоговый текстовый файл на сервере.")
                }
            } else {
                // Если произошла ошибка, просто пересылаем сообщение об ошибке пользователю
                sendReply(result.chatId, "❌ При обработке файла '${result.userFileName}' произошла ошибка.")
            }
        } catch (e: Exception) {
            sendReply(result.chatId, "❌ Произошла критическая ошибка на сервере при обработке вашего запроса.")
        }
    }
    private fun sendReply(chatId: Long, text: String) {
        try {
            val message = SendMessage(chatId.toString(), text)
            telegramClient.execute(message)
        } catch (e: Exception) {
        }
    }
}

