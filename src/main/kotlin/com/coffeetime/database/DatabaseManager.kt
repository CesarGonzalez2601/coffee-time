package com.coffeetime.database

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {

    private const val DATABASE_FOLDER = "data"

    private const val DATABASE_URL =
        "jdbc:sqlite:data/coffeetime.db"

    fun initialize() {

        createDatabaseFolder()

        getConnection().use { connection ->

            createUsersTable(connection)
        }
    }

    fun getConnection(): Connection {

        val connection =
            DriverManager.getConnection(DATABASE_URL)

        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
        }

        return connection
    }

    private fun createDatabaseFolder() {

        val folder = File(DATABASE_FOLDER)

        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    private fun createUsersTable(
        connection: Connection
    ) {

        val sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                pin_hash TEXT NOT NULL,
                pin_salt TEXT NOT NULL,
                role TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }
}