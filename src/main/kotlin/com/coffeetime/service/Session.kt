package com.coffeetime.service

import com.coffeetime.exception.PermissionDeniedException
import com.coffeetime.model.Permission
import com.coffeetime.model.User
import com.coffeetime.util.Logger

object Session {

    private var currentUser: User? = null

    fun start(user: User) {

        currentUser = user

        Logger.logInfo(
            "Session started for user ${user.id}"
        )
    }

    fun close() {

        currentUser?.let { user ->
            Logger.logInfo(
                "Session closed for user ${user.id}"
            )
        }

        currentUser = null
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    fun validatePermission(
        permission: Permission
    ) {

        val user = currentUser
            ?: throw PermissionDeniedException(
                "No active session."
            )

        if (!user.hasPermission(permission)) {

            Logger.logError(
                "Permission denied for user ${user.id}: $permission"
            )

            throw PermissionDeniedException(
                "User ${user.name} does not have permission for this action."
            )
        }
    }
}