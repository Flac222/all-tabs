package com.uade.alltabs.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.uade.alltabs.data.remote.dto.FavoriteDto
import com.uade.alltabs.data.remote.dto.TabDto
import com.uade.alltabs.data.remote.dto.UserDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        const val USERS_COLLECTION = "users"
        const val TABS_COLLECTION = "tabs"
        const val FAVORITES_COLLECTION = "favorites"
    }

    // User operations
    suspend fun saveUser(userDto: UserDto) {
        firestore.collection(USERS_COLLECTION)
            .document(userDto.uid)
            .set(userDto)
            .await()
    }

    suspend fun getUser(uid: String): UserDto? {
        return firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .await()
            .toObject(UserDto::class.java)
    }

    // Tab operations
    suspend fun saveTab(tabDto: TabDto) {
        firestore.collection(TABS_COLLECTION)
            .document(tabDto.id)
            .set(tabDto)
            .await()
    }

    suspend fun getTab(tabId: String): TabDto? {
        val doc = firestore.collection(TABS_COLLECTION)
            .document(tabId)
            .get()
            .await()
        
        return if (doc.exists()) {
            try {
                doc.toObject(TabDto::class.java)
            } catch (e: Exception) {
                mapDocumentToTabDto(doc)
            }
        } else null
    }

    fun getTabsByUserIdFlow(userId: String): kotlinx.coroutines.flow.Flow<List<TabDto>> = kotlinx.coroutines.flow.callbackFlow {
        // We listen to all tabs and filter in memory because Firestore "whereEqualTo" with a String 
        // won't match a DocumentReference.
        val registration = firestore.collection(TABS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tabs = snapshot.documents.mapNotNull { doc ->
                        val tabDto = try {
                            doc.toObject(TabDto::class.java)
                        } catch (e: Exception) {
                            mapDocumentToTabDto(doc)
                        }
                        
                        if (tabDto?.userId == userId) tabDto else null
                    }
                    trySend(tabs)
                }
            }
        awaitClose { registration.remove() }
    }

    fun getAllTabsFlow(): kotlinx.coroutines.flow.Flow<List<TabDto>> = kotlinx.coroutines.flow.callbackFlow {
        val registration = firestore.collection(TABS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tabs = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(TabDto::class.java)
                        } catch (e: Exception) {
                            mapDocumentToTabDto(doc)
                        }
                    }
                    trySend(tabs)
                }
            }
        awaitClose { registration.remove() }
    }

    private fun mapDocumentToTabDto(doc: com.google.firebase.firestore.DocumentSnapshot): TabDto {
        val userIdValue = when (val rawUserId = doc.get("userId")) {
            is String -> rawUserId
            is com.google.firebase.firestore.DocumentReference -> rawUserId.id
            else -> ""
        }
        
        return TabDto(
            id = doc.id,
            userId = userIdValue,
            userName = doc.getString("userName") ?: "",
            mbid = doc.getString("mbid"),
            titulo = doc.getString("titulo") ?: "",
            artista = doc.getString("artista") ?: "",
            acordes = doc.getString("acordes") ?: "",
            esIA = doc.getBoolean("esIA") ?: false,
            esFavorito = doc.getBoolean("esFavorito") ?: false,
            fechaCreacion = doc.getTimestamp("fechaCreacion")
        )
    }

    suspend fun getTabCountsForMbids(mbids: List<String>): Map<String, List<Pair<String, String>>> {
        if (mbids.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, MutableList<Pair<String, String>>>()

        // Firestore `in` query is limited to 10 items. Batch if necessary.
        mbids.chunked(10).forEach {
            val querySnapshot = firestore.collection(TABS_COLLECTION)
                .whereIn("mbid", it)
                .get()
                .await()
            
            for (document in querySnapshot.documents) {
                val mbid = document.getString("mbid")
                val userId = when (val rawUserId = document.get("userId")) {
                    is String -> rawUserId
                    is com.google.firebase.firestore.DocumentReference -> rawUserId.id
                    else -> null
                }
                val username = document.getString("userName") ?: "Unknown User"

                if (mbid != null && userId != null) {
                    result.getOrPut(mbid) { mutableListOf() }.add(Pair(userId, username))
                }
            }
        }
        return result
    }

    suspend fun deleteTab(tabId: String) {
        firestore.collection(TABS_COLLECTION)
            .document(tabId)
            .delete()
            .await()
    }

    // Favorite operations
    suspend fun addFavorite(favoriteDto: FavoriteDto) {
        firestore.collection(FAVORITES_COLLECTION)
            .add(favoriteDto)
            .await()
    }

    suspend fun removeFavorite(userId: String, tabId: String) {
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("tabId", tabId)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }
    }

    suspend fun getFavoritesByUserId(userId: String): List<FavoriteDto> {
        return firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                try {
                    doc.toObject(FavoriteDto::class.java)
                } catch (e: Exception) {
                    // Fallback: Manually map if there's a type mismatch (e.g., userId is a Reference)
                    val tabId = doc.getString("tabId") ?: ""
                    val titulo = doc.getString("titulo") ?: ""
                    val artista = doc.getString("artista") ?: ""
                    
                    // Handle userId as potentially a DocumentReference or String
                    val userIdValue = when (val rawUserId = doc.get("userId")) {
                        is String -> rawUserId
                        is com.google.firebase.firestore.DocumentReference -> rawUserId.id
                        else -> userId
                    }
                    
                    FavoriteDto(userId = userIdValue, tabId = tabId, titulo = titulo, artista = artista)
                }
            }
    }

    suspend fun isTabFavorited(userId: String, tabId: String): Boolean {
        return firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("tabId", tabId)
            .get()
            .await()
            .isEmpty.not()
    }
}