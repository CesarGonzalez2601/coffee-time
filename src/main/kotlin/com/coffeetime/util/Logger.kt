package com.coffeetime.util

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {

    private const val LOG_FOLDER = "logs"
    private const val LOG_FILE = "$LOG_FOLDER/errors.txt"

    private val dateFormatter =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss"
        )

    fun logError(message: String) {
        writeLog(
            level = "ERROR",
            message = message
        )
    }

    fun logInfo(message: String) {
        writeLog(
            level = "INFO",
            message = message
        )
    }

    private fun writeLog(
        level: String,
        message: String
    ) {

        val folder = File(LOG_FOLDER)

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(LOG_FILE)

        val date = LocalDateTime
            .now()
            .format(dateFormatter)

        file.appendText(
            "[$date] [$level] $message\n"
        )
    }
}