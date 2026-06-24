package com.uade.alltabs.domain.usecase

import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveTabUseCaseTest {

    private lateinit var repository: TabRepository
    private lateinit var useCase: SaveTabUseCase

    private val fakeTab = Tab(
        id = "tab-1", userId = "u1", userName = "User", mbid = null,
        titulo = "Test Song", artista = "Test Artist",
        acordes = "<score-partwise/>", esIA = false, esFavorito = false,
        fechaCreacion = 1000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SaveTabUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository saveTab`() = runTest {
        coJustRun { repository.saveTab(any()) }

        useCase(fakeTab)

        coVerify(exactly = 1) { repository.saveTab(fakeTab) }
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        coEvery { repository.saveTab(any()) } throws RuntimeException("DB write failed")

        try {
            useCase(fakeTab)
            fail("Should have thrown RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("DB write failed", e.message)
        }
    }

    @Test
    fun `invoke passes the correct tab data to repository`() = runTest {
        var capturedTab: Tab? = null
        coEvery { repository.saveTab(any()) } coAnswers {
            capturedTab = firstArg()
        }

        useCase(fakeTab)

        assertNotNull(capturedTab)
        assertEquals("tab-1", capturedTab!!.id)
        assertEquals("Test Song", capturedTab!!.titulo)
        assertEquals("Test Artist", capturedTab!!.artista)
        assertFalse(capturedTab!!.esIA)
    }

    @Test
    fun `invoke works for AI-generated tabs`() = runTest {
        val aiTab = fakeTab.copy(esIA = true, artista = "Compositor IA")
        coJustRun { repository.saveTab(any()) }

        useCase(aiTab)

        coVerify(exactly = 1) { repository.saveTab(aiTab) }
    }
}
