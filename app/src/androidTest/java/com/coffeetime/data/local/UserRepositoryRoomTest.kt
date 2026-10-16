package com.coffeetime.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coffeetime.domain.exception.UserNotFoundException
import com.coffeetime.domain.model.Administrator
import com.coffeetime.domain.model.Cashier
import com.coffeetime.domain.model.Role
import com.coffeetime.domain.service.AuthenticationService
import com.coffeetime.domain.service.Session
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Room real (en memoria) con el mismo seed que la app. Corre en emulador o teléfono. */
@RunWith(AndroidJUnit4::class)
class UserRepositoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: UserRepositoryRoom
    private lateinit var service: AuthenticationService

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            context = ApplicationProvider.getApplicationContext(),
            klass = AppDatabase::class.java
        )
            .addCallback(UserSeedCallback())
            .build()

        repository = UserRepositoryRoom(
            userDao = database.userDao()
        )
        service = AuthenticationService(
            userRepository = repository,
            session = Session()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedCreaAdministradorYCajero() = runTest {
        val users = repository.findAll()

        assertEquals(2, users.size)
        assertIs<Administrator>(users[0])
        assertEquals(UserSeedCallback.ADMIN_ID, users[0].id)
        assertIs<Cashier>(users[1])
        assertEquals(UserSeedCallback.CASHIER_ID, users[1].id)
    }

    @Test
    fun seedGuardaHashYSaltNoElPin() = runTest {
        val credentials = repository.getCredentials(UserSeedCallback.ADMIN_ID)!!

        assertTrue(credentials.pinHash != "2580")
        assertTrue(credentials.pinSalt.isNotBlank())
    }

    @Test
    fun loginContraRoomConUsuariosDePrueba() = runTest {
        assertIs<Administrator>(
            service.login(
                userId = 1,
                pin = "2580"
            )
        )
        assertIs<Cashier>(
            service.login(
                userId = 2,
                pin = "1397"
            )
        )
    }

    @Test
    fun registerAsignaElSiguienteIdComoCajero() = runTest {
        val user = service.register(
            name = "Luis",
            pin = "4826"
        )

        assertEquals(3, user.id)
        assertEquals(Role.CASHIER, user.role)
        assertEquals(3, repository.count())
        assertEquals(
            3,
            service.login(
                userId = 3,
                pin = "4826"
            ).id
        )
    }

    @Test
    fun usuarioDesactivadoNoPuedeEntrar() = runTest {
        assertTrue(
            repository.updateStatus(
                id = 2,
                active = false
            )
        )

        assertFailsWith<UserNotFoundException> {
            service.login(
                userId = 2,
                pin = "1397"
            )
        }
    }
}
