package com.mkb3ar.telegram.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient

@Configuration
class BotConfiguration {
    @Bean
    fun telegramClient(@Value("\${telegram.bot.token}") token: String) = OkHttpTelegramClient(token)
}