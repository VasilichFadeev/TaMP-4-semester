package com.example.practics_2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.practics_2.pokemons.Pokemon;
import com.example.practics_2.pokemons.PokemonDetails;
import com.example.practics_2.pokemons.PokemonList;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class PokeMain {
    public static String fetchPokemonData() {
        ObjectMapper om = new ObjectMapper();
        URI uri;
        StringBuilder result = new StringBuilder();

        try {
            uri = new URI("https://pokeapi.co/api/v2/pokemon");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        int count = 0;
        while (uri != null && count < 5) {
            try {
                URL url = uri.toURL();
                PokemonList list = om.readValue(url, PokemonList.class);

                for (Pokemon poke : list.results) {
                    result.append("Name: ").append(poke.name).append("\n");
                    result.append("URL: ").append(poke.url).append("\n");

                    PokemonDetails details = getPokemonDetails(poke.url, om);
                    if (details != null) {
                        result.append("Height: ").append(details.height).append("\n");
                        result.append("Weight: ").append(details.weight).append("\n\n");
                    }

                    count++;
                    if (count >= 5) break;
                }

                uri = (list.next != null && !list.next.isEmpty()) ? new URI(list.next) : null;
            } catch (Exception e) {
                result.append("Error: ").append(e.getMessage()).append("\n");
                break;
            }
        }

        return result.toString();
    }

    private static PokemonDetails getPokemonDetails(String url, ObjectMapper om) {
        try {
            URL pokemonUrl = new URL(url);
            return om.readValue(pokemonUrl, PokemonDetails.class);
        } catch (Exception e) {
            return null;
        }
    }
}