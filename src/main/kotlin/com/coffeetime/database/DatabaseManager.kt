package com.coffeetime.database

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {

    private data class InitialProduct(
        val name: String,
        val price: Double,
        val category: String,
        val stock: Int
    )

    private const val DATABASE_FOLDER = "data"

    private const val DATABASE_URL =
        "jdbc:sqlite:data/coffeetime.db"

    fun initialize() {

        createDatabaseFolder()

        getConnection().use { connection ->

            createUsersTable(connection)
            createProductsTable(connection)
            createOrdersTable(connection)
            createOrderDetailsTable(connection)
            insertInitialProducts(connection)
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

    private fun createProductsTable(
        connection: Connection
    ) {

        val sql = """
            CREATE TABLE IF NOT EXISTS productos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                precio REAL NOT NULL,
                categoria TEXT NOT NULL,
                stock INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun createOrdersTable(
        connection: Connection
    ) {

        val sql = """
            CREATE TABLE IF NOT EXISTS ordenes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT DEFAULT CURRENT_TIMESTAMP,
                estado TEXT NOT NULL,
                total REAL DEFAULT 0.0
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun createOrderDetailsTable(
        connection: Connection
    ) {

        val sql = """
            CREATE TABLE IF NOT EXISTS detalle_ordenes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orden_id INTEGER NOT NULL,
                producto_id INTEGER NOT NULL,
                cantidad INTEGER NOT NULL,
                subtotal REAL NOT NULL,
                FOREIGN KEY (orden_id) REFERENCES ordenes(id),
                FOREIGN KEY (producto_id) REFERENCES productos(id)
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun insertInitialProducts(
        connection: Connection
    ) {

        val countSql = "SELECT COUNT(*) FROM productos"

        connection.createStatement().use { statement ->
            statement.executeQuery(countSql).use { result ->

                if (result.next() && result.getInt(1) > 0) {
                    return
                }
            }
        }

        val sql = """
            INSERT INTO productos (nombre, precio, categoria, stock)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        val products = listOf(
            InitialProduct("Espresso", 2.50, "BEBIDAS", 25),
            InitialProduct("Cappuccino", 3.50, "BEBIDAS", 20),
            InitialProduct("Latte", 3.75, "BEBIDAS", 20),
            InitialProduct("Croissant", 2.25, "COMIDA", 15),
            InitialProduct("Cheesecake", 4.00, "POSTRES", 10),
            InitialProduct("Jarabe extra", 0.75, "EXTRAS", 30)
        )

        connection.prepareStatement(sql).use { statement ->
            products.forEach { product ->
                statement.setString(1, product.name)
                statement.setDouble(2, product.price)
                statement.setString(3, product.category)
                statement.setInt(4, product.stock)
                statement.addBatch()
            }

            statement.executeBatch()
        }
    }
}