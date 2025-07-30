package com.mkb3ar.telegram.utils // Или любой другой пакет для сервисов

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.util.concurrent.ConcurrentHashMap

@Service
class PendingFileService {
    private val pendingFiles = ConcurrentHashMap<Long, Message>()

    fun setPendingFile(chatId: Long, message: Message) {
        pendingFiles[chatId] = message
    }

    fun getAndRemovePendingFile(chatId: Long): Message? = pendingFiles.remove(chatId)
}
