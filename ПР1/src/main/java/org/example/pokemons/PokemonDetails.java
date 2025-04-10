package org.example.pokemons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PokemonDetails {
    public int height;
    public int weight;
}