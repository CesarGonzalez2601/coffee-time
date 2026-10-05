package com.coffeetime.domain.service

import android.util.Log
import com.coffeetime.domain.exception.PermissionDeniedException
import com.coffeetime.domain.model.Permission
import com.coffeetime.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Usuario con sesión iniciada. Vive solo en memoria; una instancia por app en AppContainer. */
class Session {

    private val _currentUser = MutableStateFlow<User?>(null)

    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun start(user: User) {

        _currentUser.value = user

        Log.i(
            TAG,
            "Session started for user ${user.id}"
        )
    }

    fun close() {

        _currentUser.value?.let { user ->
            Log.i(
                TAG,
                "Session closed for user ${user.id}"
            )
        }

        _currentUser.value = null
    }

    fun getCurrentUser(): User? {
        return _currentUser.value
    }

    fun validatePermission(
        permission: Permission
    ) {

        val user = _currentUser.value
            ?: throw PermissionDeniedException(
                "No active session."
            )

        if (!user.hasPermission(permission)) {

            Log.w(
                TAG,
                "Permission denied for user ${user.id}: $permission"
            )

            throw PermissionDeniedException(
                "User ${user.name} does not have permission for this action."
            )
        }
    }

    private companion object {
        const val TAG = "Session"
    }
}
