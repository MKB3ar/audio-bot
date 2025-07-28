package com.mkb3ar.telegram.bot

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient


@Component
class BotBody(
    @Value("\${telegram.bot.token}") private val token: String
): LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    private val telegramClient: TelegramClient = OkHttpTelegramClient(token)

    override fun getBotToken(): String {
        return token
    }

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer {
        return this
    }

    override fun consume(update: Update?) {
        if(update!!.hasMessage() && update.message.hasText()){
            val text: String = update.message.text
            val chatID: Long = update.message.chatId

            val sender: SendMessage = SendMessage
                .builder()
                .text(text)
                .chatId(chatID)
                .build()
            try {
                telegramClient.execute(sender)
            } catch (e: TelegramApiException){
                e.printStackTrace()
            }
        }
    }

    @AfterBotRegistration
    private fun afterRegistration(botSession: BotSession) {
        println("Registered bot running state is: " + botSession.isRunning)
    }
}