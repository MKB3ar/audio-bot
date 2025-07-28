package com.mkb3ar.telegram.utils

import org.springframework.stereotype.Component

@Component
class Extractor {
    fun extractAudio(inputVideoPath: String, outputAudioPath: String): Boolean{
        val ffmpegCommand = listOf(
            "ffmpeg",
            "-i", inputVideoPath,
            "-vn",
            "-acodec", "copy",
            outputAudioPath
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