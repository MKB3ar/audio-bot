package com.mkb3ar.telegram.utils

import org.springframework.stereotype.Component

@Component
class Extractor {
    fun extractAudio(inputVideoPath: String, outputAudioPath: String): Boolean{
        val ffmpegCommand = listOf(
            "ffmpeg",
            "-i", inputVideoPath,    // Входной файл
            "-vn",                   // Убрать видеопоток (no video)
            "-c:a", "libmp3lame",    // Явно указываем кодек для аудио (-c:a это синоним -acodec)
            "-q:a", "2",             // Устанавливаем качество (VBR), где 0 - лучшее, 9 - худшее. 2 - очень хорошо.
            outputAudioPath          // Выходной файл
        )
        try {
            val process = ProcessBuilder(ffmpegCommand)
                .inheritIO()
                .start()

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                println("Аудио успешно извлечено в: $outputAudioPath")
                return true
            } else {
                val errorOutput = process.errorStream.bufferedReader().readText()
                System.err.println("Ошибка при извлечении аудио (код $exitCode): $errorOutput")
                return false
            }
        } catch (e: Exception) {
            System.err.println("Исключение при выполнении FFmpeg: ${e.message}")
            return false
        }
    }
}