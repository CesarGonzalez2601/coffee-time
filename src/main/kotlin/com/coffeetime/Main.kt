package com.coffeetime

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.Role
import com.coffeetime.repository.ProductoRepositorySQLite
import com.coffeetime.repository.UserRepository
import com.coffeetime.repository.UserRepositorySQLite
import com.coffeetime.service.AuthenticationService
import com.coffeetime.service.OrdenService
import com.coffeetime.service.Session
import com.coffeetime.util.MenuConsole
import com.coffeetime.util.PinSecurity

fun main() {

    println("Starting Coffee Time...")

    DatabaseManager.initialize()

    val userRepository: UserRepository =
        UserRepositorySQLite()

    createInitialUsers(userRepository)

    val authenticationService =
        AuthenticationService(
            userRepository
        )

    print("Enter user ID: ")
    val userId = readln().toInt()

    print("Enter PIN: ")
    val pin = readln()

    try {

        val user = authenticationService.login(
            userId = userId,
            pin = pin
        )

        Session.start(user)

        println()
        println("Login successful")
        println("Welcome ${user.name}")
        println("Role: ${user.role}")

        val productRepository = ProductoRepositorySQLite()
        val orderService = OrdenService(productRepository)
        MenuConsole(
            productoRepository = productRepository,
            ordenService = orderService
        ).mostrarMenu()

    } catch (e: Exception) {

        println()
        println("Error: ${e.message}")
    }
}

fun createInitialUsers(
    repository: UserRepository
) {

    if (repository.count() > 0) {
        return
    }

    createUser(
        repository = repository,
        name = "Administrator",
        pin = "1234",
        role = Role.ADMINISTRATOR
    )

    createUser(
        repository = repository,
        name = "Cashier",
        pin = "4321",
        role = Role.CASHIER
    )

    println("Initial users created.")
}

fun createUser(
    repository: UserRepository,
    name: String,
    pin: String,
    role: Role
) {

    val salt =
        PinSecurity.generateSalt()

    val hash =
        PinSecurity.hashPin(
            pin = pin,
            salt = salt
        )

    repository.create(
        name = name,
        pinHash = hash,
        pinSalt = salt,
        role = role
    )
}