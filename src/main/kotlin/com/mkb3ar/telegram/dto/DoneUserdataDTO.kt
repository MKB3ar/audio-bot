package com.mkb3ar.telegram.dto

data class DoneUserdataDTO(
    val chatId: Long,
    val userFileName: String,
    val filePath: String?, // Может быть null в случае ошибки
    val status: String, // "success" или "error"
    val message: String
)
