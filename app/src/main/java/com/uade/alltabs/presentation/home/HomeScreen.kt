package com.uade.alltabs.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.uade.alltabs.R
import com.uade.alltabs.core.navigation.Screen
import com.uade.alltabs.presentation.auth.AuthViewModel
import com.uade.alltabs.presentation.components.BottomNavigationBar
import com.uade.alltabs.presentation.mytabs.TabItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showProfileMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("AllTabs", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showProfileMenu = true }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Log Out") },
                                onClick = {
                                    showProfileMenu = false
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        authViewModel.signOut()
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Search Bar Mock (navigates to SearchScreen)
            item {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.Search.route) },
                    enabled = false,
                    placeholder = { Text("¿Qué quieres tocar hoy?") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            // Quick Action Card: AI Jam
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    onClick = { navController.navigate(Screen.AiJam.route) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(
                                text = "Asistente IA",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Genera progresiones únicas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = 20.dp),
                            tint = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Recent Tabs Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tabs Recientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { navController.navigate(Screen.MyTabs.route) }) {
                        Text("Ver todo")
                    }
                }
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is HomeUiState.Success -> {
                    if (state.recentTabs.isEmpty()) {
                        item {
                            Text(
                                text = "No tienes tablaturas recientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        items(state.recentTabs) { tab ->
                            TabItemRow(tab = tab)
                        }
                    }
                }
                is HomeUiState.Error -> {
                    item {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
