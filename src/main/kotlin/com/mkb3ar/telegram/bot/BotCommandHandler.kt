package com.mkb3ar.telegram.bot

import com.mkb3ar.telegram.dto.UserDataDTO
import com.mkb3ar.telegram.entity.User
import com.mkb3ar.telegram.kafka.KafkaProducerService
import com.mkb3ar.telegram.repository.UserDataRepository
import com.mkb3ar.telegram.repository.UserRepository
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVoice
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.io.File as JavaFile

@Service
class BotCommandHandler(
    private val telegramClient: TelegramClient,
    private val userRepository: UserRepository,
    private val botMediaHandler: BotMediaHandler,
    private val userDataRepository: UserDataRepository,
    private val kafkaProducerService: KafkaProducerService
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
        if (!userRepository.existsById(chat.id)) {
            val user = User(
                id = chat.id,
                firstName = chat.firstName,
                lastName = chat.lastName,
                userName = chat.userName
            )
            userRepository.save(user)
        }

        val welcomeText = """
        Привет, ${chat.firstName}! Я твой личный помощник для хранения и расшифровки аудио 🤖
        
        **Как это работает:**
        1.  **Отправь мне аудио, голосовое или видеофайл.** Я автоматически извлеку из него аудиодорожку.
        2.  **Дай файлу имя.** Сразу после отправки я спрошу, как назвать файл. Просто ответь мне текстом.
        3.  **Управляй файлами.** Используй команду `/check`, чтобы увидеть список всех твоих файлов. Из этого списка ты сможешь прослушать аудио или получить его полную текстовую расшифровку.
        
        **Доступные команды:**
        `/check` - Показать все твои файлы, прослушать их или получить текст.
        `/start` - Показать это приветственное сообщение.
        `/cat` - Прислать фото котика (потому что все любят котиков 😻).
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
    fun handleCallbackQuery(callbackQuery: CallbackQuery) {
        val data = callbackQuery.data
        val chat = callbackQuery.message.chat
        val user = callbackQuery.from
        telegramClient.execute(AnswerCallbackQuery(callbackQuery.id))
        when {
            data.startsWith("file_") -> {
                val fileIndex = data.substringAfter("file_").toIntOrNull() ?: return

                val listenButton = InlineKeyboardButton.builder()
                    .text("🎧 Прослушать")
                    .callbackData("listen_$fileIndex")
                    .build()

                val textButton = InlineKeyboardButton.builder()
                    .text("📄 Получить текст")
                    .callbackData("text_$fileIndex")
                    .build()

                val keyboardMarkup = InlineKeyboardMarkup.builder()
                    .keyboardRow(InlineKeyboardRow(listenButton, textButton))
                    .build()

                val editMessage = EditMessageText.builder()
                    .chatId(chat.id)
                    .messageId(callbackQuery.message.messageId)
                    .text("Выберите действие для файла:")
                    .replyMarkup(keyboardMarkup)
                    .build()

                try {
                    telegramClient.execute(editMessage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            data.startsWith("listen_") -> {
                val fileIndex = data.substringAfter("listen_").toIntOrNull() ?: return

                val dbUserOptional = userRepository.findById(user.id)
                if (dbUserOptional.isEmpty) return
                val userFiles = userDataRepository.findUserDataByUser(dbUserOptional.get())

                if (fileIndex < userFiles.size) {
                    val userData = userFiles[fileIndex]
                    sendMp3AsVoice(chat, userData.filePath)
                }
            }

            // НОВАЯ ЛОГИКА
            data.startsWith("text_") -> {
                try {
                    val fileIndex = data.substringAfter("text_").toIntOrNull() ?: return

                    // Получаем информацию о файле по индексу
                    val dbUserOptional = userRepository.findById(user.id)
                    if (dbUserOptional.isEmpty) return
                    val userFiles = userDataRepository.findUserDataByUser(dbUserOptional.get())

                    if (fileIndex < userFiles.size) {
                        val userData = userFiles[fileIndex]

                        // 1. Создаем DTO с необходимой информацией
                        val request = UserDataDTO(
                            chatID = chat.id,
                            userFileName = userData.userFileName,
                            filePath = userData.filePath
                        )
                        // 2. Отправляем DTO в Kafka через наш сервис
                        kafkaProducerService.sendFileToProcess(request)

                        // 3. Сообщаем пользователю, что запрос принят
                        botMediaHandler.sendReply(chat, "✅ Запрос на получение текста из файла '${userData.userFileName}' отправлен в обработку.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    botMediaHandler.sendReply(chat, "Произошла ошибка при отправке запроса на обработку.")
                }
            }
        }
    }
    private fun sendMp3AsVoice(chat: Chat, mp3FilePath: String) {
        try {
            val sendVoice = SendVoice.builder()
                .chatId(chat.id)
                .voice(InputFile(JavaFile(mp3FilePath)))
                .build()
            telegramClient.execute(sendVoice)
            println("Тестовая отправка MP3 как голосового сообщения выполнена для файла: $mp3FilePath")
        } catch (e: Exception) {
            e.printStackTrace()
            botMediaHandler.sendReply(chat, "Произошла ошибка при попытке отправить файл как голосовое сообщение.")
        }
    }
}