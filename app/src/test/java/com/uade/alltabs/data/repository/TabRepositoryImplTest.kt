package com.uade.alltabs.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.uade.alltabs.data.local.FavoriteDao
import com.uade.alltabs.data.local.TabDao
import com.uade.alltabs.data.local.TabEntity
import com.uade.alltabs.data.remote.CoverArtArchiveApi
import com.uade.alltabs.data.remote.FirestoreService
import com.uade.alltabs.data.remote.MusicBrainzApi
import com.uade.alltabs.domain.model.Tab
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var firestoreService: FirestoreService
    private lateinit var musicBrainzApi: MusicBrainzApi
    private lateinit var coverArtArchiveApi: CoverArtArchiveApi
    private lateinit var tabDao: TabDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repository: TabRepositoryImpl

    private val fakeTab = Tab(
        id = "tab-1", userId = "u1", userName = "User", mbid = null,
        titulo = "Test Song", artista = "Test Artist",
        acordes = "", esIA = false, esFavorito = false, fechaCreacion = 1000L
    )

    private val fakeEntity = TabEntity(
        id = "tab-1", userId = "u1", userName = "User", mbid = null,
        titulo = "Test Song", artista = "Test Artist",
        acordes = "", esIA = false, esFavorito = false, fechaCreacion = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        firestoreService = mockk(relaxed = true)
        musicBrainzApi = mockk(relaxed = true)
        coverArtArchiveApi = mockk(relaxed = true)
        tabDao = mockk(relaxed = true)
        favoriteDao = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)

        // Provide a mock Context that avoids real ConnectivityManager
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns null

        repository = TabRepositoryImpl(
            firestoreService = firestoreService,
            musicBrainzApi = musicBrainzApi,
            coverArtArchiveApi = coverArtArchiveApi,
            tabDao = tabDao,
            favoriteDao = favoriteDao,
            firebaseAuth = firebaseAuth,
            ioDispatcher = testDispatcher,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── saveTab ──────────────────────────────────────────────────────────────

    @Test
    fun `saveTab writes to Room first`() = runTest {
        coJustRun { tabDao.insertTab(any()) }
        coJustRun { firestoreService.saveTab(any()) }

        repository.saveTab(fakeTab)
        advanceUntilIdle()

        coVerify(exactly = 1) { tabDao.insertTab(any()) }
    }

    @Test
    fun `saveTab then syncs to Firestore`() = runTest {
        coJustRun { tabDao.insertTab(any()) }
        coJustRun { firestoreService.saveTab(any()) }

        repository.saveTab(fakeTab)
        advanceUntilIdle()

        coVerify { firestoreService.saveTab(any()) }
    }

    @Test
    fun `saveTab still succeeds if Firestore throws (offline resilience)`() = runTest {
        coJustRun { tabDao.insertTab(any()) }
        coEvery { firestoreService.saveTab(any()) } throws RuntimeException("No internet")

        // Should not throw
        repository.saveTab(fakeTab)
        advanceUntilIdle()

        // Room was still written
        coVerify(exactly = 1) { tabDao.insertTab(any()) }
    }

    // ── getTab ───────────────────────────────────────────────────────────────

    @Test
    fun `getTab returns from Room when available`() = runTest {
        coEvery { tabDao.getTabById("tab-1") } returns fakeEntity

        val result = repository.getTab("tab-1")

        assertNotNull(result)
        assertEquals("tab-1", result!!.id)
        // Firestore should NOT be called when Room has the tab
        coVerify(exactly = 0) { firestoreService.getTab(any()) }
    }

    @Test
    fun `getTab returns null when not in Room and Firestore fails`() = runTest {
        coEvery { tabDao.getTabById(any()) } returns null
        coEvery { firestoreService.getTab(any()) } throws RuntimeException("Offline")

        val result = repository.getTab("missing-tab")
        assertNull(result)
    }

    // ── deleteTab ─────────────────────────────────────────────────────────────

    @Test
    fun `deleteTab removes from Room and Firestore`() = runTest {
        coJustRun { tabDao.deleteTab(any()) }
        coJustRun { firestoreService.deleteTab(any()) }

        repository.deleteTab("tab-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { tabDao.deleteTab("tab-1") }
        coVerify { firestoreService.deleteTab("tab-1") }
    }

    @Test
    fun `deleteTab still succeeds if Firestore throws`() = runTest {
        coJustRun { tabDao.deleteTab(any()) }
        coEvery { firestoreService.deleteTab(any()) } throws RuntimeException("No internet")

        // Should not throw
        repository.deleteTab("tab-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { tabDao.deleteTab("tab-1") }
    }

    // ── getFavoriteTabs ───────────────────────────────────────────────────────

    @Test
    fun `getFavoriteTabs returns Room Flow`() = runTest {
        every { tabDao.getFavoriteTabs(any()) } returns flowOf(listOf(fakeEntity))
        every { favoriteDao.getFavoritesForUser(any()) } returns flowOf(emptyList())

        val flow = repository.getFavoriteTabs("u1")
        var result: List<Tab>? = null
        flow.collect { result = it }

        assertNotNull(result)
        assertEquals(1, result!!.size)
    }
}
