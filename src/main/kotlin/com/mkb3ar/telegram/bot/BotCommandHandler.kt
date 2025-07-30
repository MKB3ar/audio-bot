package com.mkb3ar.telegram.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class BotCommandHandler(
    private val telegramClient: TelegramClient
){
    fun handleUnknownCommand(chat: Chat) {
        val messageText = "Я не знаю такой команды. Введите /start, чтобы увидеть список моих возможностей."
        val sendMessage = SendMessage.builder()
            .chatId(chat.id)
            .text(messageText)
            .build()

        try {
            telegramClient.execute(sendMessage)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}