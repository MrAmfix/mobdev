package io.github.mobdev.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingMessageDao {
    @Query("SELECT * FROM pending_messages WHERE channel = :channel ORDER BY localId ASC")
    suspend fun getForChannel(channel: String): List<PendingMessageEntity>

    @Insert
    suspend fun insert(msg: PendingMessageEntity): Long

    @Query("UPDATE pending_messages SET status = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: Long, status: String)

    @Query("DELETE FROM pending_messages WHERE localId = :localId")
    suspend fun delete(localId: Long)
}
