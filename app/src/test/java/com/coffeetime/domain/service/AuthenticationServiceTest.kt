package com.coffeetime.domain.service

import com.coffeetime.data.fake.FakeUserRepository
import com.coffeetime.domain.exception.IncorrectPinException
import com.coffeetime.domain.exception.UserNotFoundException
import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AuthenticationServiceTest {

    private val service = AuthenticationService(
        userRepository = FakeUserRepository()
    )

    @Test
    fun `login con ID 1 y PIN 2580 devuelve al administrador`() = runTest {
        val user = service.login(
            userId = 1,
            pin = "2580"
        )

        assertIs<Administrator>(user)
    }

    @Test
    fun `login con ID 2 y PIN 1397 devuelve al cajero`() = runTest {
        val user = service.login(
            userId = 2,
            pin = "1397"
        )

        assertIs<Cashier>(user)
    }

    @Test
    fun `login con PIN incorrecto lanza IncorrectPinException`() = runTest {
        assertFailsWith<IncorrectPinException> {
            service.login(
                userId = 1,
                pin = "0000"
            )
        }
    }

    @Test
    fun `login con ID inexistente lanza UserNotFoundException`() = runTest {
        assertFailsWith<UserNotFoundException> {
            service.login(
                userId = 99,
                pin = "2580"
            )
        }
    }
}
