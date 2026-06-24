package com.uade.alltabs.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.data.local.FavoriteDao
import com.uade.alltabs.data.local.FavoriteEntity
import com.uade.alltabs.data.local.TabDao
import com.uade.alltabs.data.local.TabEntity
import com.uade.alltabs.data.remote.CoverArtArchiveApi
import com.uade.alltabs.data.remote.FirestoreService
import com.uade.alltabs.data.remote.MusicBrainzApi
import com.uade.alltabs.data.remote.dto.FavoriteDto
import com.uade.alltabs.data.remote.dto.TabDto
import com.uade.alltabs.data.remote.dto.UserDto
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.model.User
import com.uade.alltabs.domain.repository.TabRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
class TabRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val musicBrainzApi: MusicBrainzApi,
    private val coverArtArchiveApi: CoverArtArchiveApi,
    private val tabDao: TabDao,
    private val favoriteDao: FavoriteDao,
    private val firebaseAuth: FirebaseAuth,
    private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : TabRepository {

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    syncPendingOfflineData()
                }
            })
        } catch (e: Exception) {
            // Safe fallback for custom testing environments
        }
    }

    private fun syncPendingOfflineData() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        CoroutineScope(ioDispatcher).launch {
            try {
                // 1. Sync custom local tabs
                val localTabs = tabDao.getTabsByUserIdList(currentUserId)
                localTabs.forEach { tabEntity ->
                    firestoreService.saveTab(tabEntity.toDomain().toDto())
                }

                // 2. Sync favorites
                val localFavorites = favoriteDao.getFavoritesForUserList(currentUserId)
                localFavorites.forEach { favEntity ->
                    firestoreService.addFavorite(
                        FavoriteDto(
                            userId = favEntity.userId,
                            tabId = favEntity.tabId,
                            titulo = favEntity.titulo,
                            artista = favEntity.artista
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle sync failures silently
            }
        }
    }

    override suspend fun saveUser(user: User) {
        firestoreService.saveUser(user.toDto())
    }

    override suspend fun getUser(uid: String): User? {
        return firestoreService.getUser(uid)?.toDomain()
    }

    override suspend fun saveTab(tab: Tab) {
        tabDao.insertTab(tab.toEntity())
        try {
            firestoreService.saveTab(tab.toDto())
        } catch (e: Exception) {
            // Offline resilience
        }
    }

    override suspend fun getTab(tabId: String): Tab? {
        val localTab = tabDao.getTabById(tabId)?.toDomain()
        if (localTab != null) return localTab
        
        return try {
            val remoteTab = firestoreService.getTab(tabId)?.toDomain()
            if (remoteTab != null) {
                tabDao.insertTab(remoteTab.toEntity())
            }
            remoteTab
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteTab(tabId: String) {
        tabDao.deleteTab(tabId)
        try {
            firestoreService.deleteTab(tabId)
        } catch (e: Exception) {
            // Offline resilience
        }
    }

    override suspend fun isTabFavorited(userId: String, tabId: String): Boolean {
        if (favoriteDao.isFavorite(userId, tabId)) return true
        return try {
            firestoreService.isTabFavorited(userId, tabId)
        } catch (e: Exception) {
            false
        }
    }

    override fun getTabsByUserId(userId: String): Flow<List<Tab>> {
        // Start background synchronization
        CoroutineScope(ioDispatcher).launch {
            try {
                firestoreService.getTabsByUserIdFlow(userId).collect { remoteTabs ->
                    tabDao.insertTabs(remoteTabs.map { it.toDomain().toEntity() })
                }
            } catch (e: Exception) {
                // Log or handle sync error silently for offline mode
            }
        }
        val favoriteIdsFlow = favoriteDao.getFavoritesForUser(userId)
            .map { list -> list.map { it.tabId }.toSet() }

        return combine(
            tabDao.getTabsByUserId(userId),
            favoriteIdsFlow
        ) { entities, favIds ->
            entities.map { entity ->
                entity.toDomain().copy(esFavorito = favIds.contains(entity.id))
            }
        }
    }

    override fun getFavoriteTabs(userId: String): Flow<List<Tab>> {
        syncFavoritesFromRemoteOnce(userId)
        return tabDao.getFavoriteTabs(userId).map { entities ->
            entities.map { it.toDomain().copy(esFavorito = true) }
        }
    }

    private val favoritesSyncedForUser = mutableSetOf<String>()

    private fun syncFavoritesFromRemoteOnce(userId: String) {
        synchronized(favoritesSyncedForUser) {
            if (userId in favoritesSyncedForUser) return
            favoritesSyncedForUser.add(userId)
        }
        CoroutineScope(ioDispatcher).launch {
            try {
                val remoteFavorites = firestoreService.getFavoritesByUserId(userId)
                remoteFavorites.forEach { favDto ->
                    upsertLocalFavorite(favDto.userId, favDto.tabId, favDto.titulo, favDto.artista)
                }
            } catch (e: Exception) {
                synchronized(favoritesSyncedForUser) {
                    favoritesSyncedForUser.remove(userId)
                }
            }
        }
    }

    private suspend fun upsertLocalFavorite(
        userId: String,
        tabId: String,
        titulo: String,
        artista: String
    ) {
        favoriteDao.insertFavorite(
            FavoriteEntity(
                tabId = tabId,
                userId = userId,
                titulo = titulo,
                artista = artista,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override fun getAllTabs(): Flow<List<Tab>> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        val favoriteIdsFlow = favoriteDao.getFavoritesForUser(currentUserId)
            .map { list -> list.map { it.tabId }.toSet() }

        // Start background synchronization
        CoroutineScope(ioDispatcher).launch {
            try {
                firestoreService.getAllTabsFlow().collect { remoteTabs ->
                    tabDao.insertTabs(remoteTabs.map { it.toDomain().toEntity() })
                }
            } catch (e: Exception) {
                // Log or handle sync error silently
            }
        }
        return combine(
            tabDao.getAllTabs(),
            favoriteIdsFlow
        ) { entities, favIds ->
            entities.map { entity ->
                entity.toDomain().copy(esFavorito = favIds.contains(entity.id))
            }
        }
    }

    override suspend fun getTabCountsForSongs(mbids: List<String>): Map<String, List<Pair<String, String>>> {
        return try {
            firestoreService.getTabCountsForMbids(mbids)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getSongDetailFromApi(mbid: String): Tab? {
        return try {
            val response = musicBrainzApi.getRecordingDetail(mbid)
            
            // Try to find a release-group MBID for cover art
            val firstRelease = response.releases?.firstOrNull()
            val releaseGroupId = firstRelease?.releaseGroup?.id
            
            // Try to find the earliest release date string (e.g., "1994-09-13")
            val releaseDate = response.releases?.flatMap { it.releaseEvents ?: emptyList() }
                ?.mapNotNull { it.date }
                ?.filter { it.isNotEmpty() }
                ?.minOrNull()

            Tab(
                id = response.id,
                userId = "", 
                userName = "",
                mbid = releaseGroupId ?: response.id, // Store release-group ID if available for cover art
                titulo = response.title,
                artista = response.artistCredit?.joinToString(", ") { it.name } ?: "Unknown Artist",
                acordes = releaseDate ?: "", // Carry date/year string here
                esIA = false,
                esFavorito = false,
                fechaCreacion = 0L
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addFavorite(userId: String, tabId: String, titulo: String, artista: String) {
        if (!favoriteDao.isFavorite(userId, tabId)) {
            upsertLocalFavorite(userId, tabId, titulo, artista)
        }

        tabDao.getTabById(tabId)?.let {
            tabDao.insertTab(it.copy(esFavorito = true))
        }

        try {
            val favoriteDto = FavoriteDto(userId = userId, tabId = tabId, titulo = titulo, artista = artista)
            firestoreService.addFavorite(favoriteDto)
        } catch (e: Exception) {
            // Offline resilience
        }
    }

    override suspend fun removeFavorite(userId: String, tabId: String) {
        favoriteDao.deleteFavorite(tabId, userId)
        
        tabDao.getTabById(tabId)?.let {
            tabDao.insertTab(it.copy(esFavorito = false))
        }

        try {
            firestoreService.removeFavorite(userId, tabId)
        } catch (e: Exception) {
            // Offline resilience
        }
    }

    override suspend fun getCoverArtUrl(mbid: String): String? {
        return try {
            // First try release-group as it's the "main" one
            val response = coverArtArchiveApi.getReleaseGroupCoverArt(mbid)
            response.images.firstOrNull { it.front || it.primary }?.thumbnails?.large
                ?: response.images.firstOrNull()?.thumbnails?.large
        } catch (e: Exception) {
            try {
                // Fallback to release if release-group fails
                val response = coverArtArchiveApi.getReleaseCoverArt(mbid)
                response.images.firstOrNull { it.front || it.primary }?.thumbnails?.large
                    ?: response.images.firstOrNull()?.thumbnails?.large
            } catch (e2: Exception) {
                null
            }
        }
    }

    override suspend fun fetchTabsFromApi(query: String): List<Tab> {
        return try {
            val response = musicBrainzApi.searchRecordings(query)
            response.recordings.map { recording ->
                Tab(
                    id = recording.id,
                    userId = "",
                    userName = "",
                    mbid = recording.id,
                    titulo = recording.title,
                    artista = recording.artistCredit?.joinToString(", ") { it.name } ?: "Unknown Artist",
                    acordes = "",
                    esIA = false,
                    esFavorito = false,
                    fechaCreacion = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Mappers to convert between domain model and DTOs/Entities
    private fun Tab.toEntity(): TabEntity {
        return TabEntity(
            id = id,
            userId = userId,
            userName = userName,
            mbid = mbid,
            titulo = titulo,
            artista = artista,
            acordes = acordes,
            esIA = esIA,
            esFavorito = esFavorito,
            fechaCreacion = fechaCreacion
        )
    }

    private fun TabEntity.toDomain(): Tab {
        return Tab(
            id = id,
            userId = userId,
            userName = userName,
            mbid = mbid,
            titulo = titulo,
            artista = artista,
            acordes = acordes,
            esIA = esIA,
            esFavorito = esFavorito,
            fechaCreacion = fechaCreacion
        )
    }

    private fun User.toDto(): UserDto {
        return UserDto(uid = uid, nombre = nombre, email = email, fotoUrl = fotoUrl)
    }

    private fun UserDto.toDomain(): User {
        return User(uid = uid, nombre = nombre, email = email, fotoUrl = fotoUrl)
    }

    private fun Tab.toDto(): TabDto {
        return TabDto(
            id = id,
            userId = userId,
            userName = userName,
            mbid = mbid,
            titulo = titulo,
            artista = artista,
            acordes = acordes,
            esIA = esIA,
            esFavorito = esFavorito,
            fechaCreacion = null // Use ServerTimestamp for new entries
        )
    }

    private fun TabDto.toDomain(): Tab {
        return Tab(
            id = id,
            userId = userId,
            userName = userName,
            mbid = mbid,
            titulo = titulo,
            artista = artista,
            acordes = acordes,
            esIA = esIA,
            esFavorito = esFavorito,
            fechaCreacion = fechaCreacion?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
