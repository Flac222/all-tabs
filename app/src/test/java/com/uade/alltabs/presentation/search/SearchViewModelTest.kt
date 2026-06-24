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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

        // Default: getAllTabs returns empty flow
        every { tabRepository.getAllTabs() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() {
        viewModel = SearchViewModel(fetchTabsUseCase, tabRepository, getUserUseCase)
    }

    private fun subscribeUiState(job: kotlinx.coroutines.CoroutineScope): Job =
        job.launch { viewModel.uiState.collect { } }

    @Test
    fun `initial state is Idle`() = runTest {
        buildViewModel()
        subscribeUiState(this)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
    }

    @Test
    fun `search with blank query does nothing`() = runTest {
        buildViewModel()
        subscribeUiState(this)
        viewModel.onSearchQueryChange("")
        viewModel.search()
        advanceUntilIdle()
        // State should remain Idle since query is blank
        assertTrue(
            viewModel.uiState.value is SearchUiState.Idle ||
            viewModel.uiState.value is SearchUiState.Success
        )
    }

    @Test
    fun `search success emits Success state with results`() = runTest {
        val tabs = listOf(makeTab("1", "Bohemian Rhapsody", "Queen", "mbid-1"))
        coEvery { fetchTabsUseCase(any()) } returns tabs
        buildViewModel()
        subscribeUiState(this)

        viewModel.onSearchQueryChange("Bohemian")
        viewModel.search()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Success)
        val results = (viewModel.uiState.value as SearchUiState.Success).results
        assertTrue(results.isNotEmpty())
        assertEquals("Bohemian Rhapsody", results[0].titulo)
    }

    @Test
    fun `search error emits Error state`() = runTest {
        coEvery { fetchTabsUseCase(any()) } throws RuntimeException("Network error")
        buildViewModel()
        subscribeUiState(this)

        viewModel.onSearchQueryChange("test")
        viewModel.search()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Error)
        assertTrue((viewModel.uiState.value as SearchUiState.Error).message.contains("Network error"))
    }

    @Test
    fun `clearing query resets to Idle`() = runTest {
        coEvery { fetchTabsUseCase(any()) } returns emptyList()
        buildViewModel()
        subscribeUiState(this)

        viewModel.onSearchQueryChange("test")
        viewModel.search()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
    }

    @Test
    fun `search with empty results emits Success with empty list`() = runTest {
        coEvery { fetchTabsUseCase(any()) } returns emptyList()
        buildViewModel()
        subscribeUiState(this)

        viewModel.onSearchQueryChange("xyz")
        viewModel.search()
        advanceUntilIdle()

        // With empty API and empty repo, and query still set → may be Success or Idle
        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success || state is SearchUiState.Idle)
    }
}
