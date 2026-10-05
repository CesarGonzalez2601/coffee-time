package com.coffeetime.di

import android.content.Context
import com.coffeetime.data.local.AppDatabase
import com.coffeetime.data.local.UserRepositoryRoom
import com.coffeetime.domain.repository.UserRepository
import com.coffeetime.domain.service.AuthenticationService
import com.coffeetime.domain.service.Session

/** Dependencias de la app, conectadas a mano (sin Hilt ni Koin). */
interface AppContainer {
    val userRepository: UserRepository
    val authenticationService: AuthenticationService
    val session: Session
}

class DefaultAppContainer(
    private val context: Context
) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.build(context)
    }

    override val userRepository: UserRepository by lazy {
        UserRepositoryRoom(
            userDao = database.userDao()
        )
    }

    override val session: Session = Session()

    override val authenticationService: AuthenticationService by lazy {
        AuthenticationService(
            userRepository = userRepository,
            session = session
        )
    }
}
