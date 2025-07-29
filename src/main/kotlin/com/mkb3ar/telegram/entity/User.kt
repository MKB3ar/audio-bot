package com.mkb3ar.telegram.entity

import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "user")
data class User(val id: String,
                val userID: Long,
                val firstName: String?,
                val lastName: String?,
                val userName: String?)
