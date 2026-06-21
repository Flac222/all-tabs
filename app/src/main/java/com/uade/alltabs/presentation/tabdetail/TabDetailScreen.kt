package com.uade.alltabs.presentation.tabdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.uade.alltabs.domain.model.Place
import com.uade.alltabs.core.navigation.Screen
import com.uade.alltabs.presentation.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun TabDetailScreen(
    navController: NavController,
    tabId: String?,
    viewModel: TabDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val tempo by viewModel.tempo.collectAsStateWithLifecycle()
    val isMetronomeEnabled by viewModel.isMetronomeEnabled.collectAsStateWithLifecycle()
    val currentPlayIndex by viewModel.currentPlayIndex.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Tablatura", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    val state = uiState
                    if (state is TabDetailUiState.Success) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (state.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (state.isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
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
                is TabDetailUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is TabDetailUiState.Success -> {
                    val tab = state.tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val coverUrl = tab.mbid?.let { "https://coverartarchive.org/release-group/$it/front" }
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (coverUrl != null) {
                                        GlideImage(
                                            model = coverUrl,
                                            contentDescription = "Cover",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.titulo,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tab.artista,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Creado por: ${tab.userName.ifBlank { "Usuario Desconocido" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Chord chips list
                        if (state.chords.isNotEmpty()) {
                            Text(
                                text = "Acordes de la canción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.chords.forEachIndexed { index, chord ->
                                    val isPlayingThis = index == currentPlayIndex
                                    Surface(
                                        color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = chord,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPlayingThis) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Playback Control Panel
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play / Pause Button
                                    Button(
                                        onClick = { viewModel.togglePlay() },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    // Metronome Toggle Button
                                    IconButton(
                                        onClick = { viewModel.toggleMetronome() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = if (isMetronomeEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Metrónomo",
                                            tint = if (isMetronomeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tempo Speed Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tempo: $tempo BPM",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Slider(
                                        value = tempo.toFloat(),
                                        onValueChange = { viewModel.setTempo(it.toInt()) },
                                        valueRange = 40f..240f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Guitar Tablature Grid View
                        Text(
                            text = "Tablatura (6 Cuerdas)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TablatureGrid(places = state.places, currentPlayIndex = currentPlayIndex)
                    }
                }
                is TabDetailUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { tabId?.let { viewModel.fetchTabDetails(it) } }) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ColumnScope.TablatureGrid(places: List<Place>, currentPlayIndex: Int) {
    val stringLabels = listOf("e", "B", "G", "D", "A", "E") // Standard guitar strings 1 to 6
    val scrollState = rememberScrollState()

    // Autoscroll logic to follow playback
    LaunchedEffect(currentPlayIndex) {
        if (currentPlayIndex >= 0 && places.isNotEmpty()) {
            val approxPosition = currentPlayIndex * 48 // 48dp approximate width per note column
            scrollState.animateScrollTo(approxPosition)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
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
                // Loop through guitar strings from 1 (high e) to 6 (low E)
                for (s in 1..6) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(36.dp)
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
                            // The string line drawn under the frets
                            Canvas(
                                modifier = Modifier
                                    .width((places.size * 48 + 32).dp)
                                    .height(1.dp)
                            ) {
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                                    strokeWidth = 2f
                                )
                            }

                            // Render fret positions horizontally
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                places.forEachIndexed { index, place ->
                                    val isPlayingThis = index == currentPlayIndex
                                    
                                    Box(
                                        modifier = Modifier.width(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val fretVal = if (place.isBarLine) "|" else place.slots[s - 1]
                                        if (fretVal != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .background(
                                                        color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = fretVal,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPlayingThis) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        } else {
                                            // Draw a dash if no note is played on this string at this step
                                            Text(
                                                text = "-",
                                                fontSize = 12.sp,
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
}
