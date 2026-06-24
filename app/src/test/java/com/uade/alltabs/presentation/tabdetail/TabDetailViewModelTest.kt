package com.uade.alltabs.presentation.tabdetail

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var tabRepository: TabRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var viewModel: TabDetailViewModel

    private val fakeTab = Tab(
        id = "tab-1", userId = "u1", userName = "User", mbid = null,
        titulo = "Test Tab", artista = "Artist",
        acordes = "", esIA = false, esFavorito = false, fechaCreacion = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tabRepository = mockk()
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "uid-123"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(tabId: String? = null): TabDetailViewModel {
        val savedState = SavedStateHandle(
            if (tabId != null) mapOf("tabId" to tabId) else emptyMap()
        )
        return TabDetailViewModel(tabRepository, firebaseAuth, savedState)
    }

    @Test
    fun `loadTab success emits Success state`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coEvery { tabRepository.isTabFavorited(any(), any()) } returns false

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success, got $state", state is TabDetailUiState.Success)
        assertEquals(fakeTab, (state as TabDetailUiState.Success).tab)
        assertFalse(state.isFavorited)
    }

    @Test
    fun `loadTab not found emits Error`() = runTest {
        coEvery { tabRepository.getTab(any()) } returns null

        viewModel = buildViewModel("missing-tab")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TabDetailUiState.Error)
    }

    @Test
    fun `initial isPlaying is false`() {
        viewModel = buildViewModel()
        assertFalse(viewModel.isPlaying.value)
    }

    @Test
    fun `togglePlay starts playback`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coEvery { tabRepository.isTabFavorited(any(), any()) } returns false

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        assertFalse(viewModel.isPlaying.value)
        viewModel.togglePlay()
        assertTrue(viewModel.isPlaying.value)
    }

    @Test
    fun `togglePlay twice stops playback`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coEvery { tabRepository.isTabFavorited(any(), any()) } returns false

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.togglePlay()
        assertTrue(viewModel.isPlaying.value)
        viewModel.togglePlay()
        assertFalse(viewModel.isPlaying.value)
    }

    @Test
    fun `setTempo clamps to 40-240 range`() {
        viewModel = buildViewModel()

        viewModel.setTempo(10)
        assertEquals(40, viewModel.tempo.value)

        viewModel.setTempo(300)
        assertEquals(240, viewModel.tempo.value)

        viewModel.setTempo(120)
        assertEquals(120, viewModel.tempo.value)
    }

    @Test
    fun `toggleMetronome flips isMetronomeEnabled`() {
        viewModel = buildViewModel()

        assertFalse(viewModel.isMetronomeEnabled.value)
        viewModel.toggleMetronome()
        assertTrue(viewModel.isMetronomeEnabled.value)
        viewModel.toggleMetronome()
        assertFalse(viewModel.isMetronomeEnabled.value)
    }

    @Test
    fun `toggleFavorite calls addFavorite when not favorited`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coEvery { tabRepository.isTabFavorited(any(), any()) } returnsMany listOf(false, true)
        coJustRun { tabRepository.addFavorite(any(), any(), any(), any()) }

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        val state = viewModel.uiState.value as? TabDetailUiState.Success
        assertNotNull(state)
        assertTrue(state!!.isFavorited)
    }

    @Test
    fun `toggleFavorite calls removeFavorite when already favorited`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coEvery { tabRepository.isTabFavorited(any(), any()) } returnsMany listOf(true, false)
        coJustRun { tabRepository.removeFavorite(any(), any()) }

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        val state = viewModel.uiState.value as? TabDetailUiState.Success
        assertNotNull(state)
        assertFalse(state!!.isFavorited)
    }
}
