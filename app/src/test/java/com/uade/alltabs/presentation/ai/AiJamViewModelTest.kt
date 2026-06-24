package com.uade.alltabs.presentation.ai

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uade.alltabs.domain.model.User
import com.uade.alltabs.domain.usecase.GenerateTabWithAiUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiJamViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var generateTabWithAiUseCase: GenerateTabWithAiUseCase
    private lateinit var saveTabUseCase: SaveTabUseCase
    private lateinit var getUserUseCase: GetUserUseCase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var viewModel: AiJamViewModel

    private val fakeXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <score-partwise><!-- Harmony: Am --><!-- Harmony: G --></score-partwise>
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        generateTabWithAiUseCase = mockk()
        saveTabUseCase = mockk()
        getUserUseCase = mockk()
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "uid-123"
        coEvery { getUserUseCase("uid-123") } returns User("uid-123", "Test User", "test@example.com", "")
        coJustRun { saveTabUseCase(any()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() {
        viewModel = AiJamViewModel(generateTabWithAiUseCase, saveTabUseCase, getUserUseCase, firebaseAuth)
    }

    @Test
    fun `initial state is Idle`() {
        buildViewModel()
        assertTrue(viewModel.uiState.value is AiJamUiState.Idle)
    }

    @Test
    fun `generateChords with blank prompt emits Error`() = runTest {
        buildViewModel()
        viewModel.generateChords("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AiJamUiState.Error)
        assertTrue((state as AiJamUiState.Error).message.contains("vacío"))
    }

    @Test
    fun `generateChords success emits Saved state`() = runTest {
        coEvery { generateTabWithAiUseCase(any()) } returns fakeXml
        buildViewModel()

        viewModel.generateChords("blues in A minor")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Saved but got $state", state is AiJamUiState.Saved)
    }

    @Test
    fun `generateChords failure emits Error`() = runTest {
        coEvery { generateTabWithAiUseCase(any()) } throws RuntimeException("API timeout")
        buildViewModel()

        viewModel.generateChords("jazz chord progression")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AiJamUiState.Error)
        assertTrue((state as AiJamUiState.Error).message.contains("API timeout"))
    }

    @Test
    fun `resetState resets to Idle`() = runTest {
        coEvery { generateTabWithAiUseCase(any()) } throws RuntimeException("fail")
        buildViewModel()

        viewModel.generateChords("test")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AiJamUiState.Error)

        viewModel.resetState()
        assertTrue(viewModel.uiState.value is AiJamUiState.Idle)
    }
}
