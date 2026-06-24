package com.uade.alltabs.presentation.home

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetRecentTabsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

    private suspend fun awaitSuccess(): HomeUiState.Success =
        viewModel.uiState.first { it is HomeUiState.Success } as HomeUiState.Success

    private suspend fun awaitError(): HomeUiState.Error =
        viewModel.uiState.first { it is HomeUiState.Error } as HomeUiState.Error

    @Test
    fun `emits Success with empty list when no data yet`() = runTest {
        every { getRecentTabsUseCase() } returns MutableStateFlow(emptyList())

        viewModel = HomeViewModel(getRecentTabsUseCase)

        assertTrue(awaitSuccess().recentTabs.isEmpty())
    }

    @Test
    fun `emits Success with tabs when use case returns data`() = runTest {
        val tabs = listOf(makeTab("1"), makeTab("2"), makeTab("3"))
        every { getRecentTabsUseCase() } returns flowOf(tabs)

        viewModel = HomeViewModel(getRecentTabsUseCase)
        val state = awaitSuccess()

        assertEquals(3, state.recentTabs.size)
        assertEquals("Song 1", state.recentTabs[0].titulo)
    }

    @Test
    fun `emits Success with at most 5 tabs`() = runTest {
        val tabs = (1..7).map { makeTab("$it") }
        every { getRecentTabsUseCase() } returns flowOf(tabs)

        viewModel = HomeViewModel(getRecentTabsUseCase)
        val state = awaitSuccess()

        assertEquals(5, state.recentTabs.size)
    }

    @Test
    fun `emits Success with empty list when no recent tabs`() = runTest {
        every { getRecentTabsUseCase() } returns flowOf(emptyList())

        viewModel = HomeViewModel(getRecentTabsUseCase)
        val state = awaitSuccess()

        assertEquals(0, state.recentTabs.size)
    }

    @Test
    fun `emits Error when use case throws`() = runTest {
        every { getRecentTabsUseCase() } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("DB error")
        }

        viewModel = HomeViewModel(getRecentTabsUseCase)
        val state = awaitError()

        assertTrue(state.message.contains("DB error"))
    }
}
