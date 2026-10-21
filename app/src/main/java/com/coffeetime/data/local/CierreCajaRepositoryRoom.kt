package com.coffeetime.data.local

import com.coffeetime.domain.model.CierreCaja
import com.coffeetime.domain.repository.CierreCajaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CierreCajaRepositoryRoom(
    private val cierreCajaDao: CierreCajaDao
) : CierreCajaRepository {

    override suspend fun realizarCierre(cierre: CierreCaja): Long {
        val entity = CierreCajaEntity.fromDomain(cierre)
        return cierreCajaDao.insertCierre(entity)
    }

    override fun getAllCierresFlow(): Flow<List<CierreCaja>> {
        return cierreCajaDao.getAllCierresFlow().map { list ->
            list.map { it.toDomain() }
        }
    }
}
