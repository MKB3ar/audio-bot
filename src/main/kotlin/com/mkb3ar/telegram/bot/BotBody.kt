package com.mkb3ar.telegram.bot

import com.mkb3ar.telegram.utils.PendingFileService
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
    private val pendingFileService: PendingFileService
) : LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    override fun getBotToken(): String = token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        if (update.hasMessage()){
            val message = update.message
            val chat = message.chat
            val originalFileMessage = pendingFileService.getAndRemovePendingFile(chat.id)
            if (originalFileMessage != null) {
                if (message.hasText()) {
                    val userFileName = message.text
                    botCommandHandler.handleFile(originalFileMessage, userFileName)
                    return
                } else {
                    pendingFileService.setPendingFile(chat.id, originalFileMessage)
                    botCommandHandler.handleReplyCommand(message, "Пожалуйста, введите название для файла именно текстом.")
                    return
                }
            }

            else if (update.message.hasVideo() || update.message.hasAudio() || update.message.hasVoice()){
                pendingFileService.setPendingFile(chat.id, message)
                botCommandHandler.handleReplyCommand(message, "Отлично! Как назовем этот файл?")
                return
            }

            else if(update.message.hasText() && message.text.startsWith("/")) {
                when (message.text.split(" ")[0]) {
                    "/start" -> botCommandHandler.handleStartCommand(chat)
                    "/cat" -> botCommandHandler.handlePhotoCommand(chat, "https://cataas.com/cat")
                    "/check" -> botCommandHandler.handleCheckCommand(chat)
                    else -> botCommandHandler.handleUnknownCommand(chat)
                }
                return
            }
            else {
                botCommandHandler.handleUnknownCommand(chat)
            }
        }
    }

    @AfterBotRegistration
    private fun afterRegistration(botSession: BotSession) {
        println("Registered bot running state is: " + botSession.isRunning)
    }
}
