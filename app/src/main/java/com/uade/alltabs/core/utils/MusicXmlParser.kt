package com.uade.alltabs.core.utils

import com.uade.alltabs.domain.model.Place

object MusicXmlParser {

    fun compileToMusicXml(chords: List<String>, places: List<Place>): String {
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

        places.forEachIndexed { index, place ->
            val chord = chords.getOrNull(index) ?: "N/C"
            sb.append("      <!-- Harmony: $chord -->\n")
            sb.append("      <harmony>\n")
            sb.append("        <root><root-step>${chord.take(1)}</root-step></root>\n")
            sb.append("      </harmony>\n")

            if (place.isBarLine) {
                sb.append("      <!-- BarLine -->\n")
            }
            if (place.isPalmMute) {
                sb.append("      <!-- PalmMute -->\n")
            }

            var isFirstNote = true
            for (stringIdx in 0..5) {
                val fretVal = place.slots[stringIdx]
                val stringNum = stringIdx + 1
                val isTapped = place.tapping[stringIdx]
                
                // If it's a barline or empty slot, write a place holder if we have no notes at all
                if (fretVal != null) {
                    sb.append("      <note>\n")
                    if (!isFirstNote) {
                        sb.append("        <chord/>\n")
                    }
                    val step = when (stringNum) {
                        1 -> "E"
                        2 -> "B"
                        3 -> "G"
                        4 -> "D"
                        5 -> "A"
                        else -> "E"
                    }
                    sb.append("        <pitch><step>$step</step><octave>3</octave></pitch>\n")
                    sb.append("        <duration>1</duration>\n")
                    sb.append("        <type>quarter</type>\n")
                    sb.append("        <technical>\n")
                    sb.append("          <string>$stringNum</string>\n")
                    sb.append("          <fret>$fretVal</fret>\n")
                    if (isTapped) {
                        sb.append("          <tapping/>\n")
                    }
                    sb.append("        </technical>\n")
                    sb.append("      </note>\n")
                    isFirstNote = false
                }
            }
            
            // If the place is completely empty (rest)
            if (isFirstNote) {
                sb.append("      <note>\n")
                sb.append("        <rest/>\n")
                sb.append("        <duration>1</duration>\n")
                sb.append("        <type>quarter</type>\n")
                sb.append("      </note>\n")
            }
        }

        sb.append("    </measure>\n")
        sb.append("  </part>\n")
        sb.append("</score-partwise>")
        return sb.toString()
    }

    fun parseChords(xml: String): List<String> {
        val parsedList = mutableListOf<String>()

        // 1. Try comment-based harmony representation
        val commentRegex = Regex("<!-- Harmony:\\s*(.*?)\\s*-->")
        val commentMatches = commentRegex.findAll(xml).toList()
        for (m in commentMatches) {
            parsedList.add(m.groupValues[1])
        }

        // 2. Try harmony block representation
        if (parsedList.isEmpty()) {
            val harmonyRegex = Regex("<harmony>.*?<root-step>([A-G])</root-step>.*?(?:<root-alter>(-?\\d+)</root-alter>)?.*?(?:<kind>([a-zA-Z0-9]+)</kind>)?.*?</harmony>", RegexOption.DOT_MATCHES_ALL)
            val harmonyMatches = harmonyRegex.findAll(xml).toList()
            for (match in harmonyMatches) {
                val step = match.groupValues[1]
                val alter = match.groupValues[2]
                val kind = match.groupValues[3]
                val altStr = when(alter) {
                    "1" -> "#"
                    "-1" -> "b"
                    else -> ""
                }
                val kindStr = when(kind) {
                    "minor", "min" -> "m"
                    "major", "maj" -> ""
                    "dominant", "7" -> "7"
                    else -> kind
                }
                parsedList.add("$step$altStr$kindStr")
            }
        }

        return parsedList
    }

    fun parsePlaces(xml: String): List<Place> {
        val places = mutableListOf<Place>()
        
        // Split by Harmony comments, BarLines, or Notes to group them by column (Place)
        // A simple way is to find each note and group them if they have <chord/>.
        // Let's parse all notes/comments in order.
        // We look for either notes or special comments:
        val itemRegex = Regex("<!-- Harmony:.*?-->|<!-- BarLine -->|<!-- PalmMute -->|<note>.*?</note>", RegexOption.DOT_MATCHES_ALL)
        val matches = itemRegex.findAll(xml)
        
        var currentPlace: Place? = null
        
        for (match in matches) {
            val text = match.value
            if (text.startsWith("<!-- Harmony:")) {
                // Creates a new column place
                if (currentPlace != null) {
                    places.add(currentPlace)
                }
                currentPlace = Place()
            } else if (text == "<!-- BarLine -->") {
                currentPlace?.isBarLine = true
            } else if (text == "<!-- PalmMute -->") {
                currentPlace?.isPalmMute = true
            } else if (text.startsWith("<note>")) {
                if (currentPlace == null) {
                    currentPlace = Place()
                }
                val isChord = text.contains("<chord/>")
                val isRest = text.contains("<rest/>")
                
                // If it is NOT a chord note and it's not the very first note in a Harmony slot,
                // we should start a new Place if the previous one already had notes, but usually 
                // Harmony comments demarcate column starts. If there are no harmony comments:
                if (!isChord && currentPlace.slots.any { it != null }) {
                    places.add(currentPlace)
                    currentPlace = Place()
                }
                
                if (!isRest) {
                    val stringMatch = Regex("<string>([1-6])</string>").find(text)
                    val fretMatch = Regex("<fret>([^<]+)</fret>").find(text)
                    val tappingMatch = text.contains("<tapping/>")
                    
                    val stringNum = stringMatch?.groupValues?.get(1)?.toIntOrNull()
                    val fretVal = fretMatch?.groupValues?.get(1)
                    
                    if (stringNum != null && fretVal != null) {
                        val idx = stringNum - 1
                        currentPlace.slots[idx] = fretVal
                        if (tappingMatch) {
                            currentPlace.tapping[idx] = true
                        }
                    }
                }
            }
        }
        
        if (currentPlace != null) {
            places.add(currentPlace)
        }
        
        // Fallback: If no places parsed, create a default list based on chords parsed
        if (places.isEmpty()) {
            val chords = parseChords(xml)
            chords.forEach { chord ->
                val p = Place()
                val fret = when {
                    chord.startsWith("C") -> "3"
                    chord.startsWith("A") -> "0"
                    chord.startsWith("G") -> "3"
                    chord.startsWith("F") -> "1"
                    chord.startsWith("D") -> "2"
                    chord.startsWith("E") -> "2"
                    else -> "0"
                }
                val stringNum = if (chord.startsWith("E") || chord.startsWith("G")) 6 else 5
                p.slots[stringNum - 1] = fret
                places.add(p)
            }
        }
        
        return places
    }
}
