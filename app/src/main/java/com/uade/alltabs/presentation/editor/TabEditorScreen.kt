package com.uade.alltabs.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.uade.alltabs.core.navigation.Screen
import com.uade.alltabs.presentation.components.BottomNavigationBar
import com.uade.alltabs.presentation.tabdetail.GuitarNote
import com.uade.alltabs.presentation.tabdetail.TablatureGrid

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
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    var selectedIndex by remember { mutableStateOf(-1) }
    var showAddChordDialog by remember { mutableStateOf(false) }
    var newChordName by remember { mutableStateOf("") }

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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveTab() }) {
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
                        Button(onClick = { tabId?.let { viewModel.loadTab(it) } }) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title / Metadata info
                        tab?.let {
                            Column {
                                Text(it.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(it.artista, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }

                        // Chords flow
                        Text("Progresión de Acordes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            chords.forEachIndexed { idx, chord ->
                                val isSelected = idx == selectedIndex
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { selectedIndex = idx }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = chord,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Borrar",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    viewModel.removeChord(idx)
                                                    if (selectedIndex == idx) selectedIndex = -1
                                                },
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Add Chord button
                            IconButton(
                                onClick = { showAddChordDialog = true },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Chord", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Tablature representation
                        Text("Composición de Tab (Presiona un acorde arriba para editar su nota)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TablatureGrid(notes = notes, currentPlayIndex = selectedIndex)

                        // Interactive note editor for selected chord step
                        if (selectedIndex in notes.indices) {
                            val selectedNote = notes[selectedIndex]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Editando paso ${selectedIndex + 1}: ${chords.getOrNull(selectedIndex) ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // String selection
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cuerda (1=alta, 6=baja):", fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                if (selectedNote.stringNum < 6) {
                                                    viewModel.updateNote(selectedIndex, selectedNote.stringNum + 1, selectedNote.fret)
                                                }
                                            }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Bajar cuerda", modifier = Modifier.size(20.dp))
                                            }
                                            Text(
                                                text = selectedNote.stringNum.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(onClick = {
                                                if (selectedNote.stringNum > 1) {
                                                    viewModel.updateNote(selectedIndex, selectedNote.stringNum - 1, selectedNote.fret)
                                                }
                                            }) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Subir cuerda", modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Fret selection
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Traste (0=al aire):", fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                if (selectedNote.fret > 0) {
                                                    viewModel.updateNote(selectedIndex, selectedNote.stringNum, selectedNote.fret - 1)
                                                }
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Menos traste", modifier = Modifier.size(20.dp))
                                            }
                                            Text(
                                                text = selectedNote.fret.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(onClick = {
                                                if (selectedNote.fret < 24) {
                                                    viewModel.updateNote(selectedIndex, selectedNote.stringNum, selectedNote.fret + 1)
                                                }
                                            }) {
                                                Icon(Icons.Default.Add, contentDescription = "Más traste", modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Selecciona un acorde para ver y editar la posición de su nota en el mástil.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddChordDialog) {
        AlertDialog(
            onDismissRequest = { showAddChordDialog = false },
            title = { Text("Agregar Acorde") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newChordName,
                        onValueChange = { newChordName = it },
                        label = { Text("Nombre del acorde (Ej: C, Am, G7)") },
                        singleLine = true
                    )
                    Text("Sugeridos:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("C", "G", "Am", "F", "D", "E", "A", "Em").forEach { chord ->
                            Button(
                                onClick = { newChordName = chord },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(chord, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newChordName.isNotBlank()) {
                        viewModel.addChord(newChordName.trim())
                        newChordName = ""
                        showAddChordDialog = false
                    }
                }) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
