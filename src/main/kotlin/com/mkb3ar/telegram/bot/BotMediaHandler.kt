package com.mkb3ar.telegram.bot

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.ByteArrayInputStream


@Service
class BotMediaHandler {
    fun photoFromURL(path: String): InputFile {
        val response: ResponseEntity<ByteArray> = RestClient.create()
            .get()
            .uri(path)
            .accept(MediaType.IMAGE_JPEG)
            .retrieve()
            .toEntity(ByteArray::class.java)
        return InputFile(ByteArrayInputStream(response.body), path + System.currentTimeMillis() + ".jpg")
    }
}