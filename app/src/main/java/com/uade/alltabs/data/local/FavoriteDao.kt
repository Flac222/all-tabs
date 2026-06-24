package com.uade.alltabs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    suspend fun getFavoritesForUserList(userId: String): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND tabId = :tabId)")
    suspend fun isFavorite(userId: String, tabId: String): Boolean

    @Query("DELETE FROM favorites WHERE tabId = :tabId AND userId = :userId")
    suspend fun deleteFavorite(tabId: String, userId: String)
}
