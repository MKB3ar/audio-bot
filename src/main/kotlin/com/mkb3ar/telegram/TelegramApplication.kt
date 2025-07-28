package com.mkb3ar.telegram

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TelegramApplication

fun main(args: Array<String>) {
	runApplication<TelegramApplication>(*args)
}
