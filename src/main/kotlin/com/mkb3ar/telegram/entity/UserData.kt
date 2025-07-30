package com.mkb3ar.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(value = "user_data")
data class UserData(
    @Id val id: String? = null,
    @DBRef val user: User,
    val userFileName: String,
    val filePath: String,
    val createdAt: Instant = Instant.now()
)
