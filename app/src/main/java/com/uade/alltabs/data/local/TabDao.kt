package com.uade.alltabs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun getTabById(id: String): TabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<TabEntity>)

    @Query("SELECT * FROM tabs WHERE userId = :userId")
    fun getTabsByUserId(userId: String): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE userId = :userId")
    suspend fun getTabsByUserIdList(userId: String): List<TabEntity>

    @Query("SELECT * FROM tabs WHERE id IN (SELECT tabId FROM favorites WHERE userId = :userId)")
    fun getFavoriteTabs(userId: String): Flow<List<TabEntity>>

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTab(id: String)
}
