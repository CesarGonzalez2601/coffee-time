package com.coffeetime.domain.repository

import com.coffeetime.domain.model.CierreCaja
import kotlinx.coroutines.flow.Flow

interface CierreCajaRepository {
    suspend fun realizarCierre(cierre: CierreCaja): Long
    fun getAllCierresFlow(): Flow<List<CierreCaja>>
}
