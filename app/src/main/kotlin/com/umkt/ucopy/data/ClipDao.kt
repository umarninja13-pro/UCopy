package com.umkt.ucopy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.umkt.ucopy.model.Clip
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAll(): List<Clip>

    @Query("SELECT DISTINCT category FROM clips WHERE isDeleted = 0 AND category IS NOT NULL AND category != ''")
    suspend fun getCategoryStrings(): List<String>

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

    @Query("SELECT * FROM clips WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun getTrash(): List<Clip>

    @Query("SELECT * FROM clips WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashFlow(): Flow<List<Clip>>

    @Query("DELETE FROM clips WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM clips WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun deleteOldTrash(cutoff: Long)

    @Query("UPDATE clips SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)
}
