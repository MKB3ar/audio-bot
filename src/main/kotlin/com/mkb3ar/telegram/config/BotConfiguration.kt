package com.mkb3ar.telegram.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient

@Configuration
class BotConfiguration {
    @Bean
    fun telegramClient(@Value("\${telegram.bot.token}") token: String) = OkHttpTelegramClient(token)
}