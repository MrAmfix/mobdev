package io.github.mobdev.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channel = :channel ORDER BY serverId DESC LIMIT :limit")
    suspend fun getLatest(channel: String, limit: Int = 100): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE channel = :channel AND serverId < :beforeId ORDER BY serverId DESC LIMIT :limit")
    suspend fun getOlderThan(channel: String, beforeId: Long, limit: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query(
        "DELETE FROM messages WHERE channel = :channel AND serverId NOT IN " +
        "(SELECT serverId FROM messages WHERE channel = :channel ORDER BY serverId DESC LIMIT :keep)"
    )
    suspend fun pruneOld(channel: String, keep: Int = 200)
}
