package com.uade.alltabs.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject

class GenerateTabWithAiUseCase @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    suspend operator fun invoke(prompt: String): String {
        if (prompt.isBlank()) throw IllegalArgumentException("El prompt no puede estar vacío")
        
        val response = generativeModel.generateContent(
            "You are a guitar chords and tablature assistant. Generate a guitar chord progression or arpeggio tablature in MusicXML format for the following request: $prompt.\n\n" +
            "Requirements:\n" +
            "1. Always define tuning in <staff-details> (6 strings, standard EADGBE tuning: E2, A2, D3, G3, B3, E4).\n" +
            "2. Each note must include <technical> tags with <string> (1-6, where 1 is high e and 6 is low E) and <fret> (0-24).\n" +
            "3. Wrap notes in <measure> tags and include <duration> and <type>.\n" +
            "4. Important Column and Chord/Arpeggio Rules:\n" +
            "   - We parse tablature as a sequence of columns (steps). Each column starts with a `<!-- Harmony: ChordName -->` comment (e.g. `<!-- Harmony: C -->` or `<!-- Harmony: N/C -->`).\n" +
            "   - For CHORDS (played simultaneously): Stack notes in the same column. Write a single `<!-- Harmony: ChordName -->` comment, followed by the first `<note>` (no `<chord/>` tag), followed immediately by the other notes of the chord where each subsequent note contains the `<chord/>` tag.\n" +
            "   - For ARPEGGIOS (played sequentially): Write a `<!-- Harmony: ChordName -->` (or `<!-- Harmony: N/C -->`) comment for each individual note. Each note must be in its own sequential column and MUST NOT contain a `<chord/>` tag. For example, a C major arpeggio plucking strings 5, 4, 3, 2 sequentially should have 4 separate note columns:\n" +
            "     - `<!-- Harmony: C -->` then a `<note>` with string 5, fret 3\n" +
            "     - `<!-- Harmony: C -->` then a `<note>` with string 4, fret 2\n" +
            "     - `<!-- Harmony: C -->` then a `<note>` with string 3, fret 0\n" +
            "     - `<!-- Harmony: C -->` then a `<note>` with string 2, fret 1\n" +
            "5. Keep the schema valid and minimal.\n" +
            "Output ONLY the raw XML. Do not include markdown code block formatting (like ```xml or ```), introductory, or concluding text."
        )
        val xml = response.text?.trim() ?: throw IllegalStateException("No se pudo obtener una respuesta de la IA")
        return xml.replace("```xml", "").replace("```", "").trim()
    }
}
