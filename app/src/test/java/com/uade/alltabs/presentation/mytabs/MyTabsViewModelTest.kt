package com.uade.alltabs.presentation.mytabs

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.usecase.GetFavoriteTabsUseCase
import com.uade.alltabs.domain.usecase.GetTabsByUserIdUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var getTabsByUserIdUseCase: GetTabsByUserIdUseCase
    private lateinit var getFavoriteTabsUseCase: GetFavoriteTabsUseCase
    private lateinit var viewModel: MyTabsViewModel

    private fun makeTab(id: String, userId: String = "uid-123") = Tab(
        id = id, userId = userId, userName = "User", mbid = null,
        titulo = "Song $id", artista = "Artist $id", acordes = "",
        esIA = false, esFavorito = false, fechaCreacion = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        firebaseAuth = mockk()
        firebaseUser = mockk()
        getTabsByUserIdUseCase = mockk()
        getFavoriteTabsUseCase = mockk()

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "uid-123"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): MyTabsViewModel {
        return MyTabsViewModel(firebaseAuth, getTabsByUserIdUseCase, getFavoriteTabsUseCase)
    }

    private fun subscribeUiState(job: kotlinx.coroutines.CoroutineScope): Job =
        job.launch { viewModel.uiState.collect { } }

    @Test
    fun `loads and combines user tabs and favorite tabs`() = runTest {
        val myTabs = listOf(makeTab("1"), makeTab("2"))
        val favTabs = listOf(makeTab("3", userId = "other"))
        every { getTabsByUserIdUseCase("uid-123") } returns flowOf(myTabs)
        every { getFavoriteTabsUseCase("uid-123") } returns flowOf(favTabs)

        viewModel = buildViewModel()
        subscribeUiState(this)
        advanceUntilIdle()

        val state = viewModel.uiState.value as? MyTabsUiState.Success
        assertNotNull(state)
        // 3 distinct tabs (1, 2, 3)
        assertEquals(3, state!!.tabs.size)
    }

    @Test
    fun `deduplicates tabs that appear in both user and favorites`() = runTest {
        val tab1 = makeTab("1")
        // Same tab is in both user tabs and favorites
        every { getTabsByUserIdUseCase("uid-123") } returns flowOf(listOf(tab1))
        every { getFavoriteTabsUseCase("uid-123") } returns flowOf(listOf(tab1))

        viewModel = buildViewModel()
        subscribeUiState(this)
        advanceUntilIdle()

        val state = viewModel.uiState.value as? MyTabsUiState.Success
        assertNotNull(state)
        // Only 1 unique tab after deduplication
        assertEquals(1, state!!.tabs.size)
    }

    @Test
    fun `search query filters by title`() = runTest {
        val tabs = listOf(makeTab("1"), Tab(
            id = "2", userId = "uid-123", userName = "User", mbid = null,
            titulo = "Stairway to Heaven", artista = "Led Zeppelin",
            acordes = "", esIA = false, esFavorito = false, fechaCreacion = 0L
        ))
        every { getTabsByUserIdUseCase("uid-123") } returns flowOf(tabs)
        every { getFavoriteTabsUseCase("uid-123") } returns flowOf(emptyList())

        viewModel = buildViewModel()
        subscribeUiState(this)
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Stairway")
        advanceUntilIdle()

        val state = viewModel.uiState.value as? MyTabsUiState.Success
        assertNotNull(state)
        assertEquals(1, state!!.tabs.size)
        assertEquals("Stairway to Heaven", state.tabs[0].titulo)
    }

    @Test
    fun `search query filters by artist`() = runTest {
        val tabs = listOf(
            makeTab("1"),  // artista = "Artist 1"
            Tab(
                id = "2", userId = "uid-123", userName = "User", mbid = null,
                titulo = "Any Song", artista = "Led Zeppelin",
                acordes = "", esIA = false, esFavorito = false, fechaCreacion = 0L
            )
        )
        every { getTabsByUserIdUseCase("uid-123") } returns flowOf(tabs)
        every { getFavoriteTabsUseCase("uid-123") } returns flowOf(emptyList())

        viewModel = buildViewModel()
        subscribeUiState(this)
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Led Zeppelin")
        advanceUntilIdle()

        val state = viewModel.uiState.value as? MyTabsUiState.Success
        assertNotNull(state)
        assertEquals(1, state!!.tabs.size)
    }

    @Test
    fun `empty search query shows all tabs`() = runTest {
        val tabs = listOf(makeTab("1"), makeTab("2"), makeTab("3"))
        every { getTabsByUserIdUseCase("uid-123") } returns flowOf(tabs)
        every { getFavoriteTabsUseCase("uid-123") } returns flowOf(emptyList())

        viewModel = buildViewModel()
        subscribeUiState(this)
        advanceUntilIdle()

        viewModel.onSearchQueryChange("filter something")
        advanceUntilIdle()
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        val state = viewModel.uiState.value as? MyTabsUiState.Success
        assertNotNull(state)
        assertEquals(3, state!!.tabs.size)
    }
}
