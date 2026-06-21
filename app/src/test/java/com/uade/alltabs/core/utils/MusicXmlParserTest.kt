package com.uade.alltabs.core.utils

import com.uade.alltabs.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicXmlParserTest {

    @Test
    fun testCompileAndParseChords() {
        val chords = listOf("C", "G", "Am")
        val places = listOf(
            Place(slots = mutableListOf("0", "1", "0", "2", "3", null)),
            Place(slots = mutableListOf("3", "0", "0", "0", "2", "3")),
            Place(slots = mutableListOf("0", "1", "2", "2", "0", null))
        )

        val xml = MusicXmlParser.compileToMusicXml(chords, places)
        
        // Parse chords back
        val parsedChords = MusicXmlParser.parseChords(xml)
        assertEquals(chords, parsedChords)

        // Parse places back
        val parsedPlaces = MusicXmlParser.parsePlaces(xml)
        assertEquals(places.size, parsedPlaces.size)

        // Verify C chord slots
        assertEquals("0", parsedPlaces[0].slots[0]) // high e
        assertEquals("1", parsedPlaces[0].slots[1]) // B
        assertEquals("0", parsedPlaces[0].slots[2]) // G
        assertEquals("2", parsedPlaces[0].slots[3]) // D
        assertEquals("3", parsedPlaces[0].slots[4]) // A
        assertEquals(null, parsedPlaces[0].slots[5]) // low E
    }

    @Test
    fun testAdvancedSymbolsParsing() {
        val chords = listOf("D")
        val places = listOf(
            Place(
                slots = mutableListOf("7~", "7b9", "9r7", "5/7", "7\\5", "x")
            )
        )

        val xml = MusicXmlParser.compileToMusicXml(chords, places)
        val parsedPlaces = MusicXmlParser.parsePlaces(xml)

        assertEquals(1, parsedPlaces.size)
        val firstPlace = parsedPlaces[0]

        assertEquals("7~", firstPlace.slots[0])
        assertEquals("7b9", firstPlace.slots[1])
        assertEquals("9r7", firstPlace.slots[2])
        assertEquals("5/7", firstPlace.slots[3])
        assertEquals("7\\5", firstPlace.slots[4])
        assertEquals("x", firstPlace.slots[5])
    }

    @Test
    fun testBarLinesAndPalmMuteFlags() {
        val chords = listOf("G", "N/C")
        val places = listOf(
            Place(slots = mutableListOf(null, null, null, null, null, null), isBarLine = true),
            Place(slots = mutableListOf("3", null, null, null, null, null), isPalmMute = true)
        )

        val xml = MusicXmlParser.compileToMusicXml(chords, places)
        val parsedPlaces = MusicXmlParser.parsePlaces(xml)

        assertEquals(2, parsedPlaces.size)
        assertTrue(parsedPlaces[0].isBarLine)
        assertTrue(parsedPlaces[1].isPalmMute)
        assertEquals("3", parsedPlaces[1].slots[0])
    }
}
