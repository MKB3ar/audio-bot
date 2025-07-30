package com.mkb3ar.telegram.bot

import com.mkb3ar.telegram.entity.User
import com.mkb3ar.telegram.repository.UserRepository
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Service
class BotCommandHandler(
    private val telegramClient: TelegramClient,
    private val userRepository: UserRepository,
    private val botMediaHandler: BotMediaHandler
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
    fun handleStartCommand(chat: Chat) {
        // Сохраняем пользователя, только если он новый
        if (!userRepository.existsById(chat.id.toString())) {
            val user = User(
                id = chat.id, // Используем telegram ID в качестве уникального ID документа
                firstName = chat.firstName,
                lastName = chat.lastName,
                userName = chat.userName
            )
            userRepository.save(user)
        }

        val welcomeText = """
        Привет, ${chat.firstName}! Я бот, который очень любит котиков 😻
        
        **Доступные команды:**
        /cat - прислать случайную фотографию котика
        """.trimIndent()

        val sendMessage = SendMessage.builder()
            .chatId(chat.id)
            .text(welcomeText)
            .parseMode("Markdown") // Включаем форматирование для жирного шрифта
            .build()
        try {
            telegramClient.execute(sendMessage)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
    fun handlePhotoCommand(chat: Chat, path: String) {
        try {
            val inputFile = botMediaHandler.photoFromURL(path)
            val sendPhoto = SendPhoto.builder()
                .chatId(chat.id)
                .photo(inputFile)
                .caption("Держи милоту!")
                .build()
            telegramClient.execute(sendPhoto)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = SendMessage.builder()
                .chatId(chat.id)
                .text("Ой, на сегодня милоты нет. Попробуйте еще раз позже.")
                .build()
            try {
                telegramClient.execute(errorMessage)
            } catch (apiEx: TelegramApiException) {
                apiEx.printStackTrace()
            }
        }
    }
}