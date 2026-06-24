package com.uade.alltabs.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateTabWithAiUseCaseTest {

    private lateinit var generativeModel: GenerativeModel
    private lateinit var useCase: GenerateTabWithAiUseCase

    private val validXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <score-partwise version="3.1">
            <part id="P1">
                <measure number="1">
                    <!-- Harmony: Am -->
                    <note>
                        <technical><string>1</string><fret>0</fret></technical>
                        <duration>4</duration><type>whole</type>
                    </note>
                </measure>
            </part>
        </score-partwise>
    """.trimIndent()

    @Before
    fun setUp() {
        generativeModel = mockk()
        useCase = GenerateTabWithAiUseCase(generativeModel)
    }

    @Test
    fun `invoke with blank prompt throws IllegalArgumentException`() = runTest {
        try {
            useCase("   ")
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("vacío") == true)
        }
    }

    @Test
    fun `invoke success returns MusicXML string`() = runTest {
        val response = mockk<GenerateContentResponse>()
        coEvery { response.text } returns validXml
        coEvery { generativeModel.generateContent(any<String>()) } returns response

        val result = useCase("blues chord progression in A minor")

        assertNotNull(result)
        assertTrue(result.contains("score-partwise"))
        assertTrue(result.contains("Harmony: Am"))
    }

    @Test
    fun `invoke strips markdown code fences from response`() = runTest {
        val responseWithFences = "```xml\n$validXml\n```"
        val response = mockk<GenerateContentResponse>()
        coEvery { response.text } returns responseWithFences
        coEvery { generativeModel.generateContent(any<String>()) } returns response

        val result = useCase("test prompt")

        assertFalse(result.contains("```xml"))
        assertFalse(result.contains("```"))
    }

    @Test
    fun `invoke throws when generativeModel throws`() = runTest {
        coEvery { generativeModel.generateContent(any<String>()) } throws RuntimeException("API quota exceeded")

        try {
            useCase("some prompt")
            fail("Should have thrown RuntimeException")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("API quota exceeded") == true)
        }
    }

    @Test
    fun `invoke throws IllegalStateException when response text is null`() = runTest {
        val response = mockk<GenerateContentResponse>()
        coEvery { response.text } returns null
        coEvery { generativeModel.generateContent(any<String>()) } returns response

        try {
            useCase("test")
            fail("Should have thrown IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("respuesta") == true)
        }
    }
}
