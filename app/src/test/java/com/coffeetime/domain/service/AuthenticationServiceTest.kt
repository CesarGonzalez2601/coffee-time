package com.coffeetime.domain.service

import com.coffeetime.data.fake.FakeUserRepository
import com.coffeetime.domain.exception.IncorrectPinException
import com.coffeetime.domain.exception.InvalidInputException
import com.coffeetime.domain.exception.UserNotFoundException
import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Role
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class AuthenticationServiceTest {

    private val session = Session()

    private val service = AuthenticationService(
        userRepository = FakeUserRepository(),
        session = session
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

    @Test
    fun `login correcto inicia la sesion`() = runTest {
        val user = service.login(
            userId = 2,
            pin = "1397"
        )

        assertEquals(user, session.currentUser.value)
    }

    @Test
    fun `login fallido no inicia la sesion`() = runTest {
        assertFailsWith<IncorrectPinException> {
            service.login(
                userId = 2,
                pin = "0000"
            )
        }

        assertNull(session.getCurrentUser())
    }

    @Test
    fun `logout limpia la sesion`() = runTest {
        service.login(
            userId = 1,
            pin = "2580"
        )

        service.logout()

        assertNull(session.getCurrentUser())
    }

    @Test
    fun `register crea un cajero con ID nuevo y se puede entrar con el`() = runTest {
        val registered = service.register(
            name = "  Ana María  ",
            pin = "4826"
        )

        assertIs<Cashier>(registered)
        assertEquals(Role.CASHIER, registered.role)
        assertEquals(3, registered.id)
        assertEquals("Ana María", registered.name)

        val loggedIn = service.login(
            userId = registered.id,
            pin = "4826"
        )

        assertEquals(registered.id, loggedIn.id)
    }

    @Test
    fun `register con nombre vacio lanza InvalidInputException`() = runTest {
        assertFailsWith<InvalidInputException> {
            service.register(
                name = "   ",
                pin = "4826"
            )
        }
    }

    @Test
    fun `register con PIN que no son 4 digitos lanza InvalidInputException`() = runTest {
        listOf("12a4", "123", "12345", "").forEach { pin ->
            assertFailsWith<InvalidInputException> {
                service.register(
                    name = "Ana",
                    pin = pin
                )
            }
        }
    }
}
