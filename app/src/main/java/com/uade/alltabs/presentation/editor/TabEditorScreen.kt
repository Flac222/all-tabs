package com.uade.alltabs.presentation.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.uade.alltabs.core.navigation.Screen
import com.uade.alltabs.domain.model.Place
import com.uade.alltabs.presentation.components.BottomNavigationBar

// Extract base fret numeric string (e.g. "7b9" -> "7", "12~" -> "12", "x" -> "0")
private fun getBaseFret(fretStr: String): String {
    val numeric = fretStr.takeWhile { it.isDigit() }
    return if (numeric.isBlank()) "0" else numeric
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabEditorScreen(
    navController: NavController,
    tabId: String?,
    viewModel: TabEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val chords by viewModel.chords.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val activePlaceIdx by viewModel.activePlaceIndex.collectAsStateWithLifecycle()
    val activeStringIdx by viewModel.activeStringIndex.collectAsStateWithLifecycle()

    var showFretSelector by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is TabEditorUiState.Saved) {
            navController.navigate(Screen.MyTabs.route) {
                popUpTo(Screen.MyTabs.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor de Tab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveTab() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is TabEditorUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is TabEditorUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { tabId?.let { viewModel.loadTab(it) } },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title / Metadata info
                        tab?.let {
                            Column {
                                Text(it.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(it.artista, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }

                        // Quick Chord Selection
                        Text("Insertar Acorde Rápido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("C", "G", "D", "Am", "Em").forEach { chord ->
                                Button(
                                    onClick = { viewModel.addQuickChord(chord) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(chord, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { viewModel.addEmptyPlace() },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar columna vacía", tint = MaterialTheme.colorScheme.primary)
                            }
                            // Added Spacer to prevent the '+' sign from being cut in half at the end of the scroll
                            Spacer(modifier = Modifier.width(24.dp))
                        }

                        // Tablature Grid View Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tablatura", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (places.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.removeActivePlace() },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Eliminar Columna", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Fixed height container ensures the grid and all 6 strings remain fully visible at all times
                        Card(
                            modifier = Modifier.fillMaxWidth().height(320.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            InteractiveTablatureGrid(
                                places = places,
                                activePlaceIdx = activePlaceIdx,
                                activeStringIdx = activeStringIdx,
                                onCellClick = { pIdx, sIdx ->
                                    viewModel.selectPlace(pIdx)
                                    viewModel.selectStringSlot(sIdx)
                                }
                            )
                        }

                        // Editor Actions for Selected Fret/Slot
                        if (activePlaceIdx in places.indices) {
                            val activePlace = places[activePlaceIdx]
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Column Level Modifiers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.toggleActivePlaceBarLine() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activePlace.isBarLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(
                                            "Línea de Compás (|)",
                                            color = if (activePlace.isBarLine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.toggleActivePlacePalmMute() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activePlace.isPalmMute) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(
                                            "Palm Mute (P.M.)",
                                            color = if (activePlace.isPalmMute) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Essential Note Placement actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Add Note
                                    Button(
                                        onClick = { viewModel.addNoteToActivePlace() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        enabled = activePlace.slots.any { it == null } && !activePlace.isBarLine
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nota")
                                    }

                                    // Change String
                                    Button(
                                        onClick = { viewModel.changeActiveNoteString() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        enabled = activeStringIdx != -1 && activePlace.slots[activeStringIdx] != null
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mover Cuerda")
                                    }

                                    // Delete Note
                                    Button(
                                        onClick = { viewModel.deleteNoteFromActivePlace() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        enabled = activeStringIdx != -1 && activePlace.slots[activeStringIdx] != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Borrar", color = Color.White)
                                    }
                                }

                                // Change Fret Action
                                if (activeStringIdx != -1 && activePlace.slots[activeStringIdx] != null) {
                                    Button(
                                        onClick = { showFretSelector = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Text("Modificar Fret / Fret: ${activePlace.slots[activeStringIdx]}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }

                                // Dedicated Context-sensitive Modifier Panel
                                if (activeStringIdx != -1 && activePlace.slots[activeStringIdx] != null) {
                                    Text("Panel de Modificadores", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    
                                    val currentFret = activePlace.slots[activeStringIdx] ?: "0"

                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Single-tap toggles
                                        ModifierButton(label = "Vibrato (~)", active = currentFret.endsWith("~")) {
                                            val base = getBaseFret(currentFret)
                                            if (currentFret.endsWith("~")) {
                                                viewModel.updateActiveNoteFret(base)
                                            } else {
                                                viewModel.updateActiveNoteFret("${base}~")
                                            }
                                        }

                                        ModifierButton(label = "Dead Note (x)", active = currentFret == "x") {
                                            if (currentFret == "x") {
                                                viewModel.updateActiveNoteFret("0")
                                            } else {
                                                viewModel.updateActiveNoteFret("x")
                                            }
                                        }

                                        ModifierButton(label = "Natural (< >)", active = currentFret.startsWith("<")) {
                                            val base = getBaseFret(currentFret)
                                            if (currentFret.startsWith("<")) {
                                                viewModel.updateActiveNoteFret(base)
                                            } else {
                                                viewModel.updateActiveNoteFret("<$base>")
                                            }
                                        }

                                        ModifierButton(label = "Artificial ([ ])", active = currentFret.startsWith("[")) {
                                            val base = getBaseFret(currentFret)
                                            if (currentFret.startsWith("[")) {
                                                viewModel.updateActiveNoteFret(base)
                                            } else {
                                                viewModel.updateActiveNoteFret("[$base]")
                                            }
                                        }

                                        ModifierButton(label = "Ghost (( ))", active = currentFret.startsWith("(")) {
                                            val base = getBaseFret(currentFret)
                                            if (currentFret.startsWith("(")) {
                                                viewModel.updateActiveNoteFret(base)
                                            } else {
                                                viewModel.updateActiveNoteFret("($base)")
                                            }
                                        }

                                        ModifierButton(label = "Grace (g)", active = currentFret.startsWith("g")) {
                                            val base = getBaseFret(currentFret)
                                            if (currentFret.startsWith("g")) {
                                                viewModel.updateActiveNoteFret(base)
                                            } else {
                                                viewModel.updateActiveNoteFret("g$base")
                                            }
                                        }

                                        ModifierButton(label = "Tapping (T)", active = activePlace.tapping[activeStringIdx]) {
                                            viewModel.toggleActiveNoteTapping()
                                        }

                                        // Transition buttons - prevent multiple stacking by fetching base fret
                                        ModifierButton(label = "Bend (b)", active = currentFret.contains("b")) {
                                            val base = getBaseFret(currentFret)
                                            viewModel.updateActiveNoteFret("${base}b${base.toInt() + 2}")
                                        }

                                        ModifierButton(label = "Release (r)", active = currentFret.contains("r")) {
                                            val base = getBaseFret(currentFret)
                                            viewModel.updateActiveNoteFret("${base}r${(base.toInt() - 2).coerceAtLeast(0)}")
                                        }

                                        ModifierButton(label = "Slide Up (/)", active = currentFret.contains("/")) {
                                            val base = getBaseFret(currentFret)
                                            viewModel.updateActiveNoteFret("${base}/${base.toInt() + 2}")
                                        }

                                        ModifierButton(label = "Slide Down (\\)", active = currentFret.contains("\\")) {
                                            val base = getBaseFret(currentFret)
                                            viewModel.updateActiveNoteFret("${base}\\${(base.toInt() - 2).coerceAtLeast(0)}")
                                        }

                                        ModifierButton(label = "Hammer-on (h)", active = currentFret.contains("h")) {
                                            val base = getBaseFret(currentFret)
                                            viewModel.updateActiveNoteFret("${base}h${base.toInt() + 2}")
                                        }

                                        ModifierButton(label = "Pull-off (p)", active = currentFret.contains("p")) {
                                            val base = getBaseFret(currentFret)
                                            viewModel.updateActiveNoteFret("${base}p${(base.toInt() - 2).coerceAtLeast(0)}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fret Selection Dialog
    if (showFretSelector && activePlaceIdx in places.indices && activeStringIdx != -1) {
        val currentFretStr = places[activePlaceIdx].slots[activeStringIdx] ?: "0"
        val currentNumeric = currentFretStr.filter { it.isDigit() }.toIntOrNull() ?: 0
        
        AlertDialog(
            onDismissRequest = { showFretSelector = false },
            title = { Text("Seleccionar Traste") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Traste seleccionado: $currentNumeric",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Simple grid for 0..24 selection with large targets and 0dp padding to prevent cutoff
                    Box(modifier = Modifier.height(200.dp)) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            (0..24).chunked(5).forEach { row ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                                    row.forEach { fret ->
                                        Button(
                                            onClick = {
                                                val suffix = currentFretStr.substring(currentFretStr.indexOfLast { it.isDigit() } + 1)
                                                viewModel.updateActiveNoteFret("$fret$suffix")
                                                showFretSelector = false
                                            },
                                            modifier = Modifier.size(52.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp), // Zero padding ensures 2 digits fit perfectly
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (currentNumeric == fret) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Text(
                                                text = fret.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentNumeric == fret) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFretSelector = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun ModifierButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InteractiveTablatureGrid(
    places: List<Place>,
    activePlaceIdx: Int,
    activeStringIdx: Int,
    onCellClick: (placeIdx: Int, stringIdx: Int) -> Unit
) {
    val stringLabels = listOf("e", "B", "G", "D", "A", "E") // Standard guitar strings 1 to 6
    val scrollState = rememberScrollState()

    // Autoscroll logic to follow focus
    LaunchedEffect(activePlaceIdx) {
        if (activePlaceIdx >= 0 && places.isNotEmpty()) {
            val approxPosition = activePlaceIdx * 56
            scrollState.animateScrollTo(approxPosition)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxHeight()
        ) {
            for (s in 1..6) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(48.dp)
                ) {
                    // String name label
                    Text(
                        text = stringLabels[s - 1],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Draw lines & frets horizontally
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Canvas(
                            modifier = Modifier
                                .width((places.size * 56 + 32).dp)
                                .height(1.dp)
                        ) {
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.6f),
                                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                                strokeWidth = 2f
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            places.forEachIndexed { index, place ->
                                val isPlaceActive = index == activePlaceIdx
                                val isCellSelected = isPlaceActive && (s - 1) == activeStringIdx
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            color = if (isCellSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                    else if (isPlaceActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                        )
                                        .border(
                                            width = if (isCellSelected) 2.dp else if (isPlaceActive) 1.dp else 0.dp,
                                            color = if (isCellSelected) MaterialTheme.colorScheme.primary
                                                    else if (isPlaceActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                                    else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onCellClick(index, s - 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val fretVal = if (place.isBarLine) "|" else place.slots[s - 1]
                                    if (fretVal != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    color = if (isCellSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = fretVal,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCellSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "-",
                                            fontSize = 14.sp,
                                            color = Color.Gray.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
