package com.mkb3ar.telegram.bot

import com.mkb3ar.telegram.entity.User
import org.springframework.web.reactive.function.client.WebClient
import com.mkb3ar.telegram.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.CharacterEncodingFilter
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import java.io.ByteArrayInputStream

@Component
class BotBody(
    @Value("\${telegram.bot.token}") private val token: String,
    private val telegramClient: TelegramClient,
    private val botCommandHandler: BotCommandHandler,
    private val userRepository: UserRepository
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
                    "/start" -> handleStartCommand(chat)
                    "/cat" -> handleCatCommand(chat)
                    else -> botCommandHandler.handleUnknownCommand(chat)
                }
            } else {
                // Если текст не является командой, отправляем подсказку
                botCommandHandler.handleUnknownCommand(chat)
            }
        }
    }

    private fun handleStartCommand(chat: Chat) {
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

    /**
     * Обрабатывает команду /cat.
     * Отправляет случайное изображение кота с сайта cataas.com.
     */
    private fun handleCatCommand(chat: Chat) {
        try {
            // 1. Вызываем новый метод для получения файла
            val inputFile = getCatPhotoInputFile()

            val sendPhoto = SendPhoto.builder()
                .chatId(chat.id)
                .photo(inputFile)
                .caption("Держи котика!")
                .build()

            telegramClient.execute(sendPhoto)
        } catch (e: Exception) {
            // Отлавливаем любые ошибки (сетевые или Telegram)
            e.printStackTrace()
            // Сообщаем пользователю о проблеме
            val errorMessage = SendMessage.builder()
                .chatId(chat.id)
                .text("Ой, не удалось найти котика. Попробуйте еще раз позже.")
                .build()
            try {
                telegramClient.execute(errorMessage)
            } catch (apiEx: TelegramApiException) {
                apiEx.printStackTrace()
            }
        }
    }

    /**
     * **НОВЫЙ МЕТОД**
     * Загружает изображение кота с cataas.com и преобразует его в InputFile.
     */
    private fun getCatPhotoInputFile(): InputFile {
        val photoUrl = "https://cataas.com/cat"

        // Используем WebClient для выполнения HTTP-запроса
        val webClient = WebClient.create()

        // Выполняем запрос и получаем тело ответа как массив байт.
        // .block() делает вызов синхронным, ожидая результата.
        val responseBody: ByteArray? = webClient.get()
            .uri(photoUrl)
            .accept(MediaType.IMAGE_JPEG)
            .retrieve()
            .bodyToMono(ByteArray::class.java) // Получаем тело как Mono<ByteArray>
            .block() // Блокируем выполнение, чтобы дождаться ответа

        // Проверяем, что тело ответа не пустое
        if (responseBody == null) {
            throw RuntimeException("Не удалось загрузить изображение: тело ответа пустое.")
        }

        // Создаем InputFile из полученных байтов
        val inputStream = ByteArrayInputStream(responseBody)
        val fileName = "cat${System.currentTimeMillis()}.jpg" // Генерируем уникальное имя файла

        return InputFile(inputStream, fileName)
    }

    @AfterBotRegistration
    private fun afterRegistration(botSession: BotSession) {
        println("Registered bot running state is: " + botSession.isRunning)
    }
}
