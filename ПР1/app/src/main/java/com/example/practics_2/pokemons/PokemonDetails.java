package com.example.practics_2.pokemons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PokemonDetails {
    public int height;
    public int weight;
}
