package com.uade.alltabs.data.repository

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
    private val musicBrainzApi: MusicBrainzApi
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

    override fun getTabsByUserId(userId: String): Flow<List<Tab>> = flow {
        val tabs = firestoreService.getTabsByUserId(userId).map { it.toDomain() }
        emit(tabs)
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

    override fun getAllTabs(): Flow<List<Tab>> = flow {
        val tabs = firestoreService.getAllTabs().map { it.toDomain() }
        emit(tabs)
    }

    override suspend fun addFavorite(userId: String, tabId: String, titulo: String, artista: String) {
        val favoriteDto = FavoriteDto(userId = userId, tabId = tabId, titulo = titulo, artista = artista)
        firestoreService.addFavorite(favoriteDto)
    }

    override suspend fun removeFavorite(userId: String, tabId: String) {
        firestoreService.removeFavorite(userId, tabId)
    }

    override suspend fun fetchTabsFromApi(query: String): List<Tab> {
        return try {
            val response = musicBrainzApi.searchRecordings(query)
            response.recordings.map { recording ->
                Tab(
                    id = recording.id,
                    userId = "",
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
