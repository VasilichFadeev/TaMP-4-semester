package com.example.practics_2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.practics_2.ui.theme.Practics_2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Practics_2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PokemonListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PokemonListScreen(modifier: Modifier = Modifier) {
    val pokemonListState = remember { mutableStateOf<List<PokemonInfo>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val data = withContext(Dispatchers.IO) {
                PokeMain.fetchPokemonData()
            }
            pokemonListState.value = data
            errorMessage.value = null
        } catch (e: Exception) {
            errorMessage.value = "Error loading data: ${e.localizedMessage}"
            e.printStackTrace()
        } finally {
            isLoading.value = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading.value -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            errorMessage.value != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage.value!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        isLoading.value = true
                        errorMessage.value = null
                    }) {
                        Text("Retry")
                    }
                }
            }
            pokemonListState.value.isEmpty() -> {
                Text("No Pokemon found")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(pokemonListState.value) { pokemon ->
                        Button(
                            onClick = {
                                val intent = Intent(context, PropertiesActivity::class.java).apply {
                                    putExtra("name", pokemon.name)
                                    putExtra("URL", pokemon.url)
                                    putExtra("height", pokemon.height)
                                    putExtra("weight", pokemon.weight)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = pokemon.name.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}