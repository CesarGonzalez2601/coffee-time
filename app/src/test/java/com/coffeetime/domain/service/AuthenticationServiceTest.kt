package com.coffeetime.domain.service

import com.coffeetime.data.fake.FakeAuthGateway
import com.coffeetime.data.fake.FakeUserRepository
import com.coffeetime.domain.exception.AuthenticationException
import com.coffeetime.domain.exception.InvalidInputException
import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Permission
import com.coffeetime.domain.model.Role
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticationServiceTest {

    private val gateway = FakeAuthGateway()
    private val users = FakeUserRepository()
    private val session = Session()

    private val service = AuthenticationService(
        authGateway = gateway,
        userRepository = users,
        session = session
    )

    @Test
    fun `register crea un cajero con su perfil y abre la sesion`() = runTest {
        val user = service.register(
            name = "  Ana María  ",
            email = "ana@coffeetime.test",
            password = "secreta1"
        )

        assertIs<Cashier>(user)
        assertEquals("Ana María", user.name)
        assertEquals("ana@coffeetime.test", user.email)
        assertEquals(Role.CASHIER, users.findByUid(user.uid)?.role)
        assertEquals(user.uid, session.getCurrentUser()?.uid)
    }

    @Test
    fun `register sin nombre falla antes de crear la cuenta`() = runTest {
        assertFailsWith<InvalidInputException> {
            service.register(
                name = "   ",
                email = "ana@coffeetime.test",
                password = "secreta1"
            )
        }

        assertNull(gateway.currentAccount())
    }

    @Test
    fun `register con un correo ya usado falla`() = runTest {
        service.register(
            name = "Ana",
            email = "ana@coffeetime.test",
            password = "secreta1"
        )

        val error = assertFailsWith<AuthenticationException> {
            service.register(
                name = "Otra Ana",
                email = "ana@coffeetime.test",
                password = "secreta2"
            )
        }

        assertEquals(AuthenticationException.Reason.EMAIL_ALREADY_IN_USE, error.reason)
    }

    @Test
    fun `login con correo y contrasena correctos devuelve el usuario`() = runTest {
        val registered = registerAndLogout()

        val user = service.login(
            email = " ana@coffeetime.test ",
            password = "secreta1"
        )

        assertEquals(registered.uid, user.uid)
        assertEquals(user.uid, session.getCurrentUser()?.uid)
    }

    @Test
    fun `login con contrasena incorrecta falla y no abre sesion`() = runTest {
        registerAndLogout()

        val error = assertFailsWith<AuthenticationException> {
            service.login(
                email = "ana@coffeetime.test",
                password = "otra-clave"
            )
        }

        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, error.reason)
        assertNull(session.getCurrentUser())
    }

    @Test
    fun `login sin correo falla con entrada invalida`() = runTest {
        assertFailsWith<InvalidInputException> {
            service.login(
                email = "  ",
                password = "secreta1"
            )
        }
    }

    @Test
    fun `cuenta marcada como admin en la consola entra como administrador`() = runTest {
        val registered = registerAndLogout()
        users.updateRole(
            uid = registered.uid,
            role = Role.ADMINISTRATOR
        )

        val user = service.login(
            email = "ana@coffeetime.test",
            password = "secreta1"
        )

        assertIs<Administrator>(user)
        assertTrue(user.hasPermission(Permission.MANAGE_USERS))
    }

    @Test
    fun `usuario desactivado no puede entrar y se cierra su cuenta de Firebase`() = runTest {
        val registered = registerAndLogout()
        users.updateStatus(
            uid = registered.uid,
            active = false
        )

        val error = assertFailsWith<AuthenticationException> {
            service.login(
                email = "ana@coffeetime.test",
                password = "secreta1"
            )
        }

        assertEquals(AuthenticationException.Reason.USER_DISABLED, error.reason)
        assertNull(session.getCurrentUser())
        assertNull(gateway.currentAccount())
    }

    @Test
    fun `primer acceso con Google crea un cajero con el nombre de la cuenta`() = runTest {
        val token = gateway.addGoogleAccount(
            email = "luis@gmail.com",
            displayName = "Luis Hernández"
        )

        val user = service.loginWithGoogle(token)

        assertIs<Cashier>(user)
        assertEquals("Luis Hernández", user.name)
        assertEquals(1, users.findAll().size)
    }

    @Test
    fun `segundo acceso con Google reutiliza el perfil`() = runTest {
        val token = gateway.addGoogleAccount(
            email = "luis@gmail.com",
            displayName = "Luis Hernández"
        )
        val first = service.loginWithGoogle(token)
        service.logout()

        val second = service.loginWithGoogle(token)

        assertEquals(first.uid, second.uid)
        assertEquals(1, users.findAll().size)
    }

    @Test
    fun `token de Google invalido falla`() = runTest {
        assertFailsWith<AuthenticationException> {
            service.loginWithGoogle("token-falso")
        }
    }

    @Test
    fun `restoreSession sin cuenta guardada devuelve null`() = runTest {
        assertNull(service.restoreSession())
        assertNull(session.getCurrentUser())
    }

    @Test
    fun `restoreSession con cuenta guardada abre la sesion sin pedir datos`() = runTest {
        val registered = service.register(
            name = "Ana",
            email = "ana@coffeetime.test",
            password = "secreta1"
        )
        // Simula cerrar la app: la sesión en memoria se pierde, la de Firebase no.
        session.logout()

        val restored = service.restoreSession()

        assertNotNull(restored)
        assertEquals(registered.uid, restored.uid)
        assertEquals(registered.uid, session.getCurrentUser()?.uid)
    }

    @Test
    fun `restoreSession con usuario desactivado devuelve null`() = runTest {
        val registered = service.register(
            name = "Ana",
            email = "ana@coffeetime.test",
            password = "secreta1"
        )
        session.logout()
        users.updateStatus(
            uid = registered.uid,
            active = false
        )

        assertNull(service.restoreSession())
        assertNull(gateway.currentAccount())
    }

    @Test
    fun `logout cierra la sesion local y la de Firebase`() = runTest {
        service.register(
            name = "Ana",
            email = "ana@coffeetime.test",
            password = "secreta1"
        )

        service.logout()

        assertNull(session.getCurrentUser())
        assertNull(gateway.currentAccount())
        assertNull(service.restoreSession())
    }

    @Test
    fun `sendPasswordReset envia el correo limpio`() = runTest {
        service.sendPasswordReset(" Ana@CoffeeTime.test ")

        assertEquals(listOf("ana@coffeetime.test"), gateway.passwordResets)
    }

    @Test
    fun `sendPasswordReset sin correo falla`() = runTest {
        assertFailsWith<InvalidInputException> {
            service.sendPasswordReset("")
        }

        assertFalse(gateway.passwordResets.isNotEmpty())
    }

    private suspend fun registerAndLogout() =
        service.register(
            name = "Ana",
            email = "ana@coffeetime.test",
            password = "secreta1"
        ).also {
            service.logout()
        }
}
