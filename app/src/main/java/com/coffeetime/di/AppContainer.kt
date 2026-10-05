package com.coffeetime.di

import com.coffeetime.data.fake.FakeUserRepository
import com.coffeetime.domain.repository.UserRepository
import com.coffeetime.domain.service.AuthenticationService
import com.coffeetime.domain.service.Session

/** Dependencias de la app, conectadas a mano (sin Hilt ni Koin). */
interface AppContainer {
    val userRepository: UserRepository
    val authenticationService: AuthenticationService
    val session: Session
}

class DefaultAppContainer : AppContainer {

    // #8 reemplaza esto por el repositorio de Room.
    override val userRepository: UserRepository by lazy {
        FakeUserRepository()
    }

    override val authenticationService: AuthenticationService by lazy {
        AuthenticationService(
            userRepository = userRepository
        )
    }

    override val session: Session = Session()
}
