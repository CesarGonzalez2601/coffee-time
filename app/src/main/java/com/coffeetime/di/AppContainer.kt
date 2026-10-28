package com.coffeetime.di

import android.content.Context
import com.coffeetime.data.fake.FakeAuthGateway
import com.coffeetime.data.fake.FakeUserRepository
import com.coffeetime.data.local.AppDatabase
import com.coffeetime.data.local.CierreCajaRepositoryRoom
import com.coffeetime.data.local.InventoryRepositoryRoom
import com.coffeetime.data.local.OrderRepositoryRoom
import com.coffeetime.data.local.PaymentRepositoryRoom
import com.coffeetime.data.local.ProductRepositoryRoom
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

    private val database: AppDatabase by lazy {
        AppDatabase.build(context)
    }

    // Temporal: se cambian por FirebaseAuthGateway y UserRepositoryFirestore (#23).
    private val authGateway: AuthGateway by lazy {
        FakeAuthGateway()
    }

    override val userRepository: UserRepository by lazy {
        FakeUserRepository()
    }

    override val productRepository: ProductRepository by lazy {
        ProductRepositoryRoom(
            productDao = database.productDao()
        )
    }

    override val orderRepository: OrderRepository by lazy {
        OrderRepositoryRoom(
            orderDao = database.orderDao()
        )
    }

    override val paymentRepository: PaymentRepository by lazy {
        PaymentRepositoryRoom(
            paymentDao = database.paymentDao()
        )
    }

    override val inventoryRepository: InventoryRepository by lazy {
        InventoryRepositoryRoom(
            inventoryMovementDao = database.inventoryMovementDao()
        )
    }

    override val cierreCajaRepository: CierreCajaRepository by lazy {
        CierreCajaRepositoryRoom(
            cierreCajaDao = database.cierreCajaDao()
        )
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
