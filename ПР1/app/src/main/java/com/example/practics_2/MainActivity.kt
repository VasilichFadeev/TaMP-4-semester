package com.example.practics_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.practics_2.ui.theme.Practics_2Theme

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
    val pokemonData = remember { mutableStateOf("Loading...") }

    // Загрузка данных из строки
    Thread {
        val data = PokeMain.fetchPokemonData()
        pokemonData.value = data
    }.start()

    LazyColumn(modifier = modifier) {
        items(pokemonData.value.split("\n")) { line ->
            Text(text = line)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PokemonListPreview() {
    Practics_2Theme {
        PokemonListScreen()
    }
}