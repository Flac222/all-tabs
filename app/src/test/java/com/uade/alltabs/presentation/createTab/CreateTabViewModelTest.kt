package com.uade.alltabs.presentation.createTab

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uade.alltabs.domain.model.User
import com.uade.alltabs.domain.usecase.GetUserUseCase
import com.uade.alltabs.domain.usecase.SaveTabUseCase
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
class CreateTabViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var saveTabUseCase: SaveTabUseCase
    private lateinit var getUserUseCase: GetUserUseCase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var viewModel: CreateTabViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        saveTabUseCase = mockk()
        getUserUseCase = mockk()
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "uid-123"
        coEvery { getUserUseCase("uid-123") } returns User("uid-123", "Test User", "test@test.com", "")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): CreateTabViewModel {
        return CreateTabViewModel(saveTabUseCase, getUserUseCase, firebaseAuth)
    }

    @Test
    fun `initial state is Idle`() {
        viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value is CreateTabUiState.Idle)
    }

    @Test
    fun `createTab with blank title emits Error`() = runTest {
        viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createCustomTab(titulo = "   ", artista = "Artist")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CreateTabUiState.Error)
        assertTrue((state as CreateTabUiState.Error).message.contains("obligatorios"))
    }

    @Test
    fun `createTab with blank artist emits Error`() = runTest {
        viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createCustomTab(titulo = "My Song", artista = "")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CreateTabUiState.Error)
    }

    @Test
    fun `createTab success emits Success with tabId`() = runTest {
        coJustRun { saveTabUseCase(any()) }
        viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createCustomTab(titulo = "My Song", artista = "My Artist")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success, got $state", state is CreateTabUiState.Success)
        assertNotNull((state as CreateTabUiState.Success).tabId)
        assertTrue(state.tabId.isNotBlank())
    }

    @Test
    fun `createTab failure emits Error`() = runTest {
        coEvery { saveTabUseCase(any()) } throws RuntimeException("Save failed")
        viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createCustomTab(titulo = "My Song", artista = "My Artist")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CreateTabUiState.Error)
    }

    @Test
    fun `userName is populated from getUserUseCase`() = runTest {
        viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("Test User", viewModel.userName.value)
    }
}
