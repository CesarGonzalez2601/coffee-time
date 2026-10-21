package com.coffeetime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CierreCajaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCierre(cierre: CierreCajaEntity): Long

    @Query("SELECT * FROM ${CierreCajaEntity.TABLE} ORDER BY timestamp DESC")
    fun getAllCierresFlow(): Flow<List<CierreCajaEntity>>
}
