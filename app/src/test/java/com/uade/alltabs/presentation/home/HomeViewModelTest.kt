package com.uade.alltabs.presentation.home

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetRecentTabsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var getRecentTabsUseCase: GetRecentTabsUseCase
    private lateinit var viewModel: HomeViewModel

    private fun makeTab(id: String) = Tab(
        id = id, userId = "u1", userName = "User", mbid = null,
        titulo = "Song $id", artista = "Artist", acordes = "", esIA = false,
        esFavorito = false, fechaCreacion = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getRecentTabsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** WhileSubscribed stateIn only collects when observed — mirror Compose collectAsState. */
    private fun subscribeUiState(job: kotlinx.coroutines.CoroutineScope): Job =
        job.launch { viewModel.uiState.collect { } }

    @Test
    fun `initial state is Loading`() = runTest {
        every { getRecentTabsUseCase() } returns MutableStateFlow(emptyList())

        viewModel = HomeViewModel(getRecentTabsUseCase)
        subscribeUiState(this)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HomeUiState.Success)
    }

    @Test
    fun `emits Success with tabs when use case returns data`() = runTest {
        // Arrange
        val tabs = listOf(makeTab("1"), makeTab("2"), makeTab("3"))
        every { getRecentTabsUseCase() } returns flowOf(tabs)

        viewModel = HomeViewModel(getRecentTabsUseCase)
        subscribeUiState(this)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        val successState = state as HomeUiState.Success
        assertEquals(3, successState.recentTabs.size)
        assertEquals("Song 1", successState.recentTabs[0].titulo)
    }

    @Test
    fun `emits Success with at most 5 tabs`() = runTest {
        // Arrange: 7 tabs, only 5 should appear
        val tabs = (1..7).map { makeTab("$it") }
        every { getRecentTabsUseCase() } returns flowOf(tabs)

        viewModel = HomeViewModel(getRecentTabsUseCase)
        subscribeUiState(this)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(5, state.recentTabs.size)
    }

    @Test
    fun `emits Success with empty list when no recent tabs`() = runTest {
        // Arrange
        every { getRecentTabsUseCase() } returns flowOf(emptyList())

        viewModel = HomeViewModel(getRecentTabsUseCase)
        subscribeUiState(this)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        assertEquals(0, (state as HomeUiState.Success).recentTabs.size)
    }

    @Test
    fun `emits Error when use case throws`() = runTest {
        // Arrange
        every { getRecentTabsUseCase() } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("DB error")
        }

        viewModel = HomeViewModel(getRecentTabsUseCase)
        subscribeUiState(this)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertTrue((state as HomeUiState.Error).message.contains("DB error"))
    }
}
