package com.coffeetime.data.local

import com.coffeetime.domain.model.Pago
import com.coffeetime.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class PaymentRepositoryRoom(
    private val paymentDao: PaymentDao
) : PaymentRepository {

    override suspend fun processPayment(pago: Pago): String {
        val count = paymentDao.getCount() + 1
        val correlative = pago.comprobanteCorrelativo.ifBlank {
            String.format(Locale.US, "TICKET-%04d", count)
        }

        val entity = PaymentEntity(
            id = pago.id,
            orderId = pago.ordenId,
            paymentMethod = pago.metodoPago.name,
            amountReceived = pago.montoRecibido,
            change = pago.cambio,
            correlative = correlative,
            timestamp = pago.fechaTimestamp,
            cashierId = pago.cajeroId
        )

        paymentDao.insertPayment(entity)
        return correlative
    }

    override fun getAllPaymentsFlow(): Flow<List<Pago>> {
        return paymentDao.getAllPaymentsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPaymentForOrder(orderId: Int): Pago? {
        return paymentDao.getPaymentForOrder(orderId)?.toDomain()
    }
}
