package com.uade.alltabs.data.repository

import com.uade.alltabs.data.remote.CoverArtArchiveApi
import com.uade.alltabs.data.remote.FirestoreService
import com.uade.alltabs.data.remote.MusicBrainzApi
import com.uade.alltabs.data.remote.dto.FavoriteDto
import com.uade.alltabs.data.remote.dto.TabDto
import com.uade.alltabs.data.remote.dto.UserDto
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.model.User
import com.uade.alltabs.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val musicBrainzApi: MusicBrainzApi,
    private val coverArtArchiveApi: CoverArtArchiveApi
) : TabRepository {

    override suspend fun saveUser(user: User) {
        firestoreService.saveUser(user.toDto())
    }

    override suspend fun getUser(uid: String): User? {
        return firestoreService.getUser(uid)?.toDomain()
    }

    override suspend fun saveTab(tab: Tab) {
        firestoreService.saveTab(tab.toDto())
    }

    override suspend fun getTab(tabId: String): Tab? {
        return firestoreService.getTab(tabId)?.toDomain()
    }

    override suspend fun deleteTab(tabId: String) {
        firestoreService.deleteTab(tabId)
    }

    override suspend fun isTabFavorited(userId: String, tabId: String): Boolean {
        return firestoreService.isTabFavorited(userId, tabId)
    }

    override fun getTabsByUserId(userId: String): Flow<List<Tab>> {
        return firestoreService.getTabsByUserIdFlow(userId).map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override fun getFavoriteTabs(userId: String): Flow<List<Tab>> = flow {
        val favoriteDtos = firestoreService.getFavoritesByUserId(userId)
        val favoriteTabIds = favoriteDtos.map { it.tabId }
        val favoriteTabs = mutableListOf<Tab>()
        favoriteTabIds.forEach { tabId ->
            firestoreService.getTab(tabId)?.let { favoriteTabs.add(it.toDomain()) }
        }
        emit(favoriteTabs)
    }

    override fun getAllTabs(): Flow<List<Tab>> {
        return firestoreService.getAllTabsFlow().map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun getTabCountsForSongs(mbids: List<String>): Map<String, List<Pair<String, String>>> {
        return firestoreService.getTabCountsForMbids(mbids)
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

            // We'll use the 'acordes' field of the Tab object temporarily to carry the year/date string
            // from the API result to the ViewModel, as this is a "virtual" Tab for song details.
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
        val favoriteDto = FavoriteDto(userId = userId, tabId = tabId, titulo = titulo, artista = artista)
        firestoreService.addFavorite(favoriteDto)
    }

    override suspend fun removeFavorite(userId: String, tabId: String) {
        firestoreService.removeFavorite(userId, tabId)
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

    // Mappers to convert between domain model and DTOs
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
