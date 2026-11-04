package com.coffeetime.di

import android.content.Context
import com.coffeetime.data.firebase.CierreCajaRepositoryFirestore
import com.coffeetime.data.firebase.FirebaseAuthGateway
import com.coffeetime.data.firebase.InventoryRepositoryFirestore
import com.coffeetime.data.firebase.OrderRepositoryFirestore
import com.coffeetime.data.firebase.PaymentRepositoryFirestore
import com.coffeetime.data.firebase.ProductRepositoryFirestore
import com.coffeetime.data.firebase.UserRepositoryFirestore
import com.coffeetime.domain.repository.CierreCajaRepository
import com.coffeetime.domain.repository.InventoryRepository
import com.coffeetime.domain.repository.OrderRepository
import com.coffeetime.domain.repository.PaymentRepository
import com.coffeetime.domain.repository.ProductRepository
import com.coffeetime.domain.repository.UserRepository
import com.coffeetime.domain.service.AuthGateway
import com.coffeetime.domain.service.AuthenticationService
import com.coffeetime.domain.service.Session

/** Dependencias de la app, conectadas a mano (sin Hilt ni Koin). */
interface AppContainer {
    val userRepository: UserRepository
    val productRepository: ProductRepository
    val orderRepository: OrderRepository
    val paymentRepository: PaymentRepository
    val inventoryRepository: InventoryRepository
    val cierreCajaRepository: CierreCajaRepository
    val authenticationService: AuthenticationService
    val session: Session
}

class DefaultAppContainer(
    private val context: Context
) : AppContainer {

    private val authGateway: AuthGateway by lazy {
        FirebaseAuthGateway()
    }

    override val userRepository: UserRepository by lazy {
        UserRepositoryFirestore()
    }

    override val productRepository: ProductRepository by lazy {
        ProductRepositoryFirestore()
    }

    override val orderRepository: OrderRepository by lazy {
        OrderRepositoryFirestore()
    }

    override val paymentRepository: PaymentRepository by lazy {
        PaymentRepositoryFirestore()
    }

    override val inventoryRepository: InventoryRepository by lazy {
        InventoryRepositoryFirestore()
    }

    override val cierreCajaRepository: CierreCajaRepository by lazy {
        CierreCajaRepositoryFirestore()
    }

    override val session: Session = Session()

    override val authenticationService: AuthenticationService by lazy {
        AuthenticationService(
            authGateway = authGateway,
            userRepository = userRepository,
            session = session
        )
    }
}
