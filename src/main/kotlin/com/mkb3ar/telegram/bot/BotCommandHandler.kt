package com.mkb3ar.telegram.bot

import com.mkb3ar.telegram.entity.User
import com.mkb3ar.telegram.repository.UserDataRepository
import com.mkb3ar.telegram.repository.UserRepository
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Service
class BotCommandHandler(
    private val telegramClient: TelegramClient,
    private val userRepository: UserRepository,
    private val botMediaHandler: BotMediaHandler,
    private val userDataRepository: UserDataRepository
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
        if (!userRepository.existsById(chat.id)) {
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
    fun handleReplyCommand(message: Message, text: String){
        val sendMessage: SendMessage = SendMessage
            .builder()
            .chatId(message.chatId)
            .text(text)
            .replyToMessageId(message.messageId)
            .build()
        try {
            telegramClient.execute(sendMessage)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
    fun handleFile(message: Message, userFileName: String): Boolean{
        return when {
            message.hasVideo() -> {
                botMediaHandler.saveVideo(message.chat, message.video, userFileName)
                true
            }
            message.hasAudio() -> {
                botMediaHandler.saveAudio(message.chat, message.audio, userFileName)
                true
            }
            message.hasVoice() -> {
                botMediaHandler.saveVoice(message.chat, message.voice, userFileName)
                true
            }
            else -> false
        }
    }
    fun handleCheckCommand(chat: Chat){
        val userFiles = userDataRepository.findUserDataByUser(userRepository.findUserById(chat.id))
        val keyboardRows = mutableListOf<InlineKeyboardRow>()
        userFiles.forEachIndexed { index, userData ->
            val button = InlineKeyboardButton
                .builder()
                .text(userData.userFileName)
                .callbackData("file_${index}")
                .build()
            keyboardRows.add(InlineKeyboardRow(button))
        }
        val keyboardMarkup = InlineKeyboardMarkup.builder().keyboard(keyboardRows).build()
        val message = SendMessage.builder()
            .chatId(chat.id)
            .text("Какой файл вы хотите обработать?")
            .replyMarkup(keyboardMarkup)
            .build()
        try {
            telegramClient.execute(message)
        } catch (e: TelegramApiException){
            e.printStackTrace()
        }
    }
}