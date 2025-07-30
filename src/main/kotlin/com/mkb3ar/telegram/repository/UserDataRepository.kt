package com.mkb3ar.telegram.repository

import com.mkb3ar.telegram.entity.User
import com.mkb3ar.telegram.entity.UserData
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserDataRepository: MongoRepository<UserData, String> {
    fun findUserDataByUser(user: User): List<UserData>
}