package com.uade.alltabs.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject

class GenerateTabWithAiUseCase @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    suspend operator fun invoke(prompt: String): String {
        if (prompt.isBlank()) return getFallbackMusicXml("C Am F G")
        return try {
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
            val xml = response.text?.trim() ?: getFallbackMusicXml("C Am F G")
            xml.replace("```xml", "").replace("```", "").trim()
        } catch (e: Exception) {
            val p = prompt.lowercase()
            val chords = when {
                p.contains("sad") || p.contains("triste") || p.contains("minor") || p.contains("menor") -> "Am Dm G C F Bm7b5 E7"
                p.contains("happy") || p.contains("alegre") || p.contains("rock") -> "G C D G"
                p.contains("jazz") -> "Dm7 G7 Cmaj7 A7"
                p.contains("blues") -> "A7 D7 A7 E7 D7 A7"
                else -> "C G Am F"
            }
            getFallbackMusicXml(chords)
        }
    }

    private fun getFallbackMusicXml(chords: String): String {
        val chordList = chords.split(" ").filter { it.isNotBlank() }
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<score-partwise version=\"3.1\">\n")
        sb.append("  <part-list>\n")
        sb.append("    <score-part id=\"P1\">\n")
        sb.append("      <part-name>Guitar</part-name>\n")
        sb.append("    </score-part>\n")
        sb.append("  </part-list>\n")
        sb.append("  <part id=\"P1\">\n")
        sb.append("    <measure number=\"1\">\n")
        sb.append("      <attributes>\n")
        sb.append("        <divisions>1</divisions>\n")
        sb.append("        <key><fifths>0</fifths></key>\n")
        sb.append("        <time><beats>4</beats><beat-type>4</beat-type></time>\n")
        sb.append("        <clef><sign>TAB</sign><line>5</line></clef>\n")
        sb.append("        <staff-details>\n")
        sb.append("          <staff-lines>6</staff-lines>\n")
        sb.append("          <staff-tuning line=\"1\"><tuning-step>E</tuning-step><tuning-octave>4</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"2\"><tuning-step>B</tuning-step><tuning-octave>3</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"3\"><tuning-step>G</tuning-step><tuning-octave>3</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"4\"><tuning-step>D</tuning-step><tuning-octave>3</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"5\"><tuning-step>A</tuning-step><tuning-octave>2</tuning-octave></staff-tuning>\n")
        sb.append("          <staff-tuning line=\"6\"><tuning-step>E</tuning-step><tuning-octave>2</tuning-octave></staff-tuning>\n")
        sb.append("        </staff-details>\n")
        sb.append("      </attributes>\n")
        
        chordList.forEach { chord ->
            sb.append("      <!-- Harmony: $chord -->\n")
            sb.append("      <harmony>\n")
            sb.append("        <root><root-step>${chord.take(1)}</root-step></root>\n")
            sb.append("      </harmony>\n")
            // Add a mock note for visual Representation
            sb.append("      <note>\n")
            sb.append("        <pitch><step>${chord.take(1)}</step><octave>3</octave></pitch>\n")
            sb.append("        <duration>4</duration>\n")
            sb.append("        <type>whole</type>\n")
            sb.append("        <technical>\n")
            sb.append("          <string>5</string>\n")
            sb.append("          <fret>3</fret>\n")
            sb.append("        </technical>\n")
            sb.append("      </note>\n")
        }
        
        sb.append("    </measure>\n")
        sb.append("  </part>\n")
        sb.append("</score-partwise>")
        return sb.toString()
    }
}
