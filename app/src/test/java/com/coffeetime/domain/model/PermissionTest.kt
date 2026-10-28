package com.coffeetime.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PermissionTest {

    @Test
    fun `el cajero solo puede crear ordenes y cobrar`() {
        val cashier = Cashier(
            uid = "2",
            name = "Cajero",
            email = "cajero@coffeetime.test"
        )

        val allowed = Permission.entries.filter { cashier.hasPermission(it) }

        assertEquals(
            listOf(Permission.CREATE_ORDER, Permission.PROCESS_PAYMENT),
            allowed
        )
    }

    @Test
    fun `el administrador tiene todos los permisos`() {
        val admin = Administrator(
            uid = "1",
            name = "Administrador",
            email = "admin@coffeetime.test"
        )

        assertTrue(Permission.entries.all { admin.hasPermission(it) })
    }
}
