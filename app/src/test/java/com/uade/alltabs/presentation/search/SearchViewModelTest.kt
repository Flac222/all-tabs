package com.uade.alltabs.presentation.search

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.domain.usecase.FetchTabsUseCase
import com.uade.alltabs.domain.usecase.GetUserUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fetchTabsUseCase: FetchTabsUseCase
    private lateinit var tabRepository: TabRepository
    private lateinit var getUserUseCase: GetUserUseCase
    private lateinit var viewModel: SearchViewModel

    private fun makeTab(id: String, titulo: String = "Song $id", artista: String = "Artist", mbid: String = id) = Tab(
        id = id, userId = "u1", userName = "User", mbid = mbid,
        titulo = titulo, artista = artista, acordes = "", esIA = false,
        esFavorito = false, fechaCreacion = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fetchTabsUseCase = mockk()
        tabRepository = mockk()
        getUserUseCase = mockk()

        every { tabRepository.getAllTabs() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() {
        viewModel = SearchViewModel(fetchTabsUseCase, tabRepository, getUserUseCase)
    }

    private suspend fun awaitIdle(): SearchUiState.Idle =
        viewModel.uiState.first { it is SearchUiState.Idle } as SearchUiState.Idle

    private suspend fun awaitSuccess(): SearchUiState.Success =
        viewModel.uiState.first { it is SearchUiState.Success } as SearchUiState.Success

    private suspend fun awaitError(): SearchUiState.Error =
        viewModel.uiState.first { it is SearchUiState.Error } as SearchUiState.Error

    @Test
    fun `initial state is Idle`() = runTest {
        buildViewModel()
        awaitIdle()
    }

    @Test
    fun `search with blank query does nothing`() = runTest {
        buildViewModel()
        awaitIdle()

        viewModel.onSearchQueryChange("")
        viewModel.search()

        awaitIdle()
    }

    @Test
    fun `search success emits Success state with results`() = runTest {
        val tabs = listOf(makeTab("1", "Bohemian Rhapsody", "Queen", "mbid-1"))
        coEvery { fetchTabsUseCase(any()) } returns tabs
        buildViewModel()
        awaitIdle()

        viewModel.onSearchQueryChange("Bohemian")
        viewModel.search()

        val state = awaitSuccess()
        assertTrue(state.results.isNotEmpty())
        assertEquals("Bohemian Rhapsody", state.results[0].titulo)
    }

    @Test
    fun `search error emits Error state`() = runTest {
        coEvery { fetchTabsUseCase(any()) } throws RuntimeException("Network error")
        buildViewModel()
        awaitIdle()

        viewModel.onSearchQueryChange("test")
        viewModel.search()

        val state = awaitError()
        assertTrue(state.message.contains("Network error"))
    }

    @Test
    fun `clearing query resets to Idle`() = runTest {
        coEvery { fetchTabsUseCase(any()) } returns emptyList()
        buildViewModel()
        awaitIdle()

        viewModel.onSearchQueryChange("test")
        viewModel.search()
        awaitSuccess()

        viewModel.onSearchQueryChange("")
        awaitIdle()
    }

    @Test
    fun `search with empty results emits Success with empty list`() = runTest {
        coEvery { fetchTabsUseCase(any()) } returns emptyList()
        buildViewModel()
        awaitIdle()

        viewModel.onSearchQueryChange("xyz")
        viewModel.search()

        val state = awaitSuccess()
        assertTrue(state.results.isEmpty())
    }
}
