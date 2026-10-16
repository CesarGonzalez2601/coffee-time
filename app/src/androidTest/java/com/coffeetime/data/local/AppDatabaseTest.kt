package com.coffeetime.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** La base en archivo, construida igual que en la app: el seed corre una sola vez. */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun seedNoSeRepiteAlReabrirLaBase() = runTest {
        val first = AppDatabase.build(context)
        assertEquals(2, first.userDao().count())
        first.close()

        val reopened = AppDatabase.build(context)
        assertEquals(2, reopened.userDao().count())
        reopened.close()
    }

    private companion object {
        const val DB_NAME = "coffeetime.db"
    }
}
