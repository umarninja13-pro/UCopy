package com.umkt.ucopy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.umkt.ucopy.model.Clip

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAll(): List<Clip>

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getById(id: String): Clip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Clip)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Clip>)

    @Update
    suspend fun update(item: Clip)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clips WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM clips")
    suspend fun deleteAll()
}
