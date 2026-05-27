package com.uade.alltabs.domain.repository

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    suspend fun saveUser(user: User)
    suspend fun getUser(uid: String): User?

    suspend fun saveTab(tab: Tab)
    suspend fun getTab(tabId: String): Tab?
    suspend fun deleteTab(tabId: String)
    suspend fun isTabFavorited(userId: String, tabId: String): Boolean

    // Flow for observing changes in tabs (for 'My Tabs' and 'Favorites')
    fun getTabsByUserId(userId: String): Flow<List<Tab>>
    fun getFavoriteTabs(userId: String): Flow<List<Tab>>
    fun getAllTabs(): Flow<List<Tab>> // For searching all public tabs, if applicable
    suspend fun getTabCountsForSongs(mbids: List<String>): Map<String, List<Pair<String, String>>>
    suspend fun getSongDetailFromApi(mbid: String): Tab? // Assuming Tab can represent basic song detail

    suspend fun addFavorite(userId: String, tabId: String, titulo: String, artista: String)
    suspend fun removeFavorite(userId: String, tabId: String)

    suspend fun fetchTabsFromApi(query: String): List<Tab>
    suspend fun getCoverArtUrl(mbid: String): String?
}
