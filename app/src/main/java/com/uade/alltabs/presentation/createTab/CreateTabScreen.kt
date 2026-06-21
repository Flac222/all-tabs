package com.uade.alltabs.presentation.createTab

import androidx.compose.foundation.layout.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.uade.alltabs.core.navigation.Screen
import com.uade.alltabs.presentation.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTabScreen(
    navController: NavController,
    initialTitle: String? = null,
    initialArtist: String? = null,
    mbid: String? = null,
    viewModel: CreateTabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    
    var titulo by remember { mutableStateOf(initialTitle ?: "") }
    var artista by remember { mutableStateOf(initialArtist ?: "") }
    
    val isArtistLocked = initialArtist != null

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is CreateTabUiState.Success) {
            navController.navigate(Screen.TabEditor.createRoute(state.tabId)) {
                popUpTo(Screen.CreateTab.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Tab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Comienza una nueva composición",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Option 1: Choose existing song (Hide if a song is already selected)
            if (!isArtistLocked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(Screen.Search.route) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Elegir canción existente", fontWeight = FontWeight.Bold)
                            Text(text = "Busca en la base de datos de MusicBrainz", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = "Canción seleccionada de la biblioteca",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Custom song / Selected song form
            Text(
                text = if (isArtistLocked) "Detalles de la Tab" else "O crea una personalizada", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título de la tab") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                placeholder = { if (isArtistLocked) Text(initialTitle ?: "") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = artista,
                onValueChange = { artista = it },
                label = { Text("Artista / Banda") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                enabled = !isArtistLocked,
                colors = if (isArtistLocked) OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) else OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Autor: $userName", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is CreateTabUiState.Error) {
                Text(
                    text = (uiState as CreateTabUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.createCustomTab(titulo, artista, mbid) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = uiState !is CreateTabUiState.Loading
            ) {
                if (uiState is CreateTabUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Comenzar Composición")
                }
            }
        }
    }
}
