package com.mkb3ar.telegram.repository

import com.mkb3ar.telegram.entity.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: MongoRepository<User, Long>{
    fun findUserById(id: Long): User
}