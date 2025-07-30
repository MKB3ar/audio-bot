package com.mkb3ar.telegram.bot

import com.mkb3ar.telegram.entity.User
import com.mkb3ar.telegram.entity.UserData
import com.mkb3ar.telegram.repository.UserDataRepository
import com.mkb3ar.telegram.repository.UserRepository
import com.mkb3ar.telegram.utils.Extractor
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.*
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Service
class BotMediaHandler(
    private val telegramClient: TelegramClient,
    private val extractor: Extractor,
    private val userDataRepository: UserDataRepository,
    private val userRepository: UserRepository,
    @Value("\${telegram.bot.token}") private val token: String
) {
    fun photoFromURL(path: String): InputFile {
        val response: ResponseEntity<ByteArray> = RestClient.create()
            .get()
            .uri(path)
            .accept(MediaType.IMAGE_JPEG)
            .retrieve()
            .toEntity(ByteArray::class.java)
        return InputFile(ByteArrayInputStream(response.body), path + System.currentTimeMillis() + ".jpg")
    }

    fun saveAudio(chat: Chat, audio: Audio, fileName: String) = saveMediaFile(chat, audio.fileId, fileName)
    fun saveVoice(chat: Chat, voice: Voice,fileName: String) = saveMediaFile(chat, voice.fileId, fileName)

    fun saveVideo(chat: Chat, video: Video, fileName: String) {
        var tempVideoFile: Path? = null
        try {
            sendReply(chat, "Получил видео. Начинаю обработку аудио.")
            val videoFile: File = telegramClient.execute(GetFile(video.fileId))
            val videoFileBytes = RestClient
                .create()
                .get()
                .uri(videoFile.getFileUrl(token))
                .retrieve()
                .body(ByteArray::class.java)
            if (videoFileBytes == null) {
                sendReply(chat, "Не удалось скачать видеофайл.")
                return
            }
            tempVideoFile = Files.createTempFile("video_", ".${videoFile.filePath?.substringAfterLast('.') ?: "mp4"}")
            if (tempVideoFile != null) {
                Files.write(tempVideoFile, videoFileBytes)
            }
            val outputDir = Paths.get("uploads/${chat.id}", "audio")
            Files.createDirectories(outputDir)
            val outputAudioPath = outputDir.resolve("${chat.id}_${System.currentTimeMillis()}.mp3")

            val success = extractor.extractAudio(tempVideoFile.toString(), outputAudioPath.toString())

            if (success) {
                sendReply(chat, "Аудиодорожка успешно извлечена и сохранена!")
                saveUserData(chat, fileName, outputAudioPath.toString())
                println("Аудио сохранено в: $outputAudioPath")
            } else {
                sendReply(chat, "Не удалось извлечь аудиодорожку из видео.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            sendReply(chat, "Произошла критическая ошибка при обработке видео.")
        } finally {
            tempVideoFile?.let {
                try {
                    Files.deleteIfExists(it)
                    println("Временный файл удален: $it")
                } catch (e: Exception) {
                    System.err.println("Не удалось удалить временный файл: $it. Ошибка: ${e.message}")
                }
            }
        }
    }

    private fun saveMediaFile(chat: Chat, fileID: String, fileName: String){
        sendReply(chat, "Процесс сохранения файла успешно запущен.")
        try{
            val file: File = telegramClient.execute(GetFile(fileID))
            val fileByte = RestClient
                .create()
                .get()
                .uri(file.getFileUrl(token))
                .retrieve()
                .body(ByteArray::class.java)

            val uploadDir = Paths.get("uploads/${chat.id}", "audio") // Создаем подпапку для каждого типа контента и для каждого пользователя
            Files.createDirectories(uploadDir)
            val savePath = uploadDir.resolve("${chat.id}_${System.currentTimeMillis()}.mp3")
            if (fileByte != null) {
                Files.write(savePath, fileByte)
            }
            println("Файл успешно сохранен: $savePath")
            saveUserData(chat, fileName, savePath.toString())
            sendReply(chat, "Файл был успешно сохранен.")
        } catch (e: Exception) {
            e.printStackTrace()
            sendReply(chat, "В процессе сохранения произошла ошибка.")
        }
    }

    private fun sendReply(chat: Chat, text: String) {
        val message = SendMessage(chat.id.toString(), text)
        try {
            telegramClient.execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun saveUserData(chat: Chat, userFileName: String, filePath: String){
        val user: User = userRepository.findUserById(chat.id)
        val userData = UserData(user = user, userFileName = userFileName, filePath = filePath)
        userDataRepository.save(userData)
    }
}