package com.mkb3ar.telegram.bot

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class BotBody(
    @Value("\${telegram.bot.token}") private val token: String,
    private val botCommandHandler: BotCommandHandler,
) : LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    override fun getBotToken(): String = token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        // Проверяем, есть ли в обновлении сообщение с текстом
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val messageText = message.text
            val chat = message.chat

            // Обрабатываем только команды, начинающиеся с "/"
            if (messageText.startsWith("/")) {
                when (messageText.split(" ")[0]) {
                    "/start" -> botCommandHandler.handleStartCommand(chat)
                    "/cat" -> botCommandHandler.handlePhotoCommand(chat, "https://cataas.com/cat")
                    else -> botCommandHandler.handleUnknownCommand(chat)
                }
            } else {
                botCommandHandler.handleUnknownCommand(chat)
            }
        }
    }

    @AfterBotRegistration
    private fun afterRegistration(botSession: BotSession) {
        println("Registered bot running state is: " + botSession.isRunning)
    }
}
