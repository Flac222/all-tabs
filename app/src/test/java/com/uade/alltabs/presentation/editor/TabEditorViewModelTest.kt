package com.uade.alltabs.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.uade.alltabs.domain.model.Place
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var tabRepository: TabRepository
    private lateinit var viewModel: TabEditorViewModel

    private val fakeTab = Tab(
        id = "tab-1", userId = "u1", userName = "User", mbid = null,
        titulo = "Test Tab", artista = "Artist",
        acordes = "", esIA = false, esFavorito = false, fechaCreacion = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tabRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(tabId: String? = null): TabEditorViewModel {
        val savedState = SavedStateHandle(
            if (tabId != null) mapOf("tabId" to tabId) else emptyMap()
        )
        return TabEditorViewModel(tabRepository, savedState)
    }

    @Test
    fun `initial state is Idle when no tabId`() {
        viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value is TabEditorUiState.Idle)
    }

    @Test
    fun `loadTab success transitions to Success state`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success, got $state", state is TabEditorUiState.Success)
        assertEquals(fakeTab, (state as TabEditorUiState.Success).tab)
    }

    @Test
    fun `loadTab not found emits Error`() = runTest {
        coEvery { tabRepository.getTab(any()) } returns null

        viewModel = buildViewModel("tab-missing")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TabEditorUiState.Error)
    }

    @Test
    fun `addEmptyPlace appends a new Place`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        val before = viewModel.places.value.size
        viewModel.addEmptyPlace()

        assertEquals(before + 1, viewModel.places.value.size)
    }

    @Test
    fun `removeActivePlace removes the selected place`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.addEmptyPlace()
        viewModel.addEmptyPlace()
        val before = viewModel.places.value.size

        viewModel.selectPlace(0)
        viewModel.removeActivePlace()

        assertEquals(before - 1, viewModel.places.value.size)
    }

    @Test
    fun `addQuickChord appends chord with correct slots`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        val before = viewModel.places.value.size
        viewModel.addQuickChord("C")

        assertEquals(before + 1, viewModel.places.value.size)
        val lastPlace = viewModel.places.value.last()
        // C chord: slots = ["0","1","0","2","3",null]
        assertEquals("0", lastPlace.slots[0])
        assertEquals("1", lastPlace.slots[1])
        assertEquals("3", lastPlace.slots[4])
        assertNull(lastPlace.slots[5])

        val lastChord = viewModel.chords.value.last()
        assertEquals("C", lastChord)
    }

    @Test
    fun `saveTab success emits Saved state`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coJustRun { tabRepository.saveTab(any()) }

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.saveTab()
        advanceUntilIdle()

        assertTrue("Expected Saved, got ${viewModel.uiState.value}", viewModel.uiState.value is TabEditorUiState.Saved)
    }

    @Test
    fun `saveTab failure emits Error`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        coEvery { tabRepository.saveTab(any()) } throws RuntimeException("DB write failed")

        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.saveTab()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TabEditorUiState.Error)
    }

    @Test
    fun `selectPlace updates activePlaceIndex`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.addEmptyPlace()
        viewModel.addEmptyPlace()
        viewModel.selectPlace(1)

        assertEquals(1, viewModel.activePlaceIndex.value)
    }

    @Test
    fun `selectStringSlot updates activeStringIndex`() = runTest {
        coEvery { tabRepository.getTab("tab-1") } returns fakeTab
        viewModel = buildViewModel("tab-1")
        advanceUntilIdle()

        viewModel.selectStringSlot(3)
        assertEquals(3, viewModel.activeStringIndex.value)
    }
}
