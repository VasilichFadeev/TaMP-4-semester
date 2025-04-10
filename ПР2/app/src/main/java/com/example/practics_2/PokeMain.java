package com.example.practics_2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.practics_2.pokemons.Pokemon;
import com.example.practics_2.pokemons.PokemonDetails;
import com.example.practics_2.pokemons.PokemonList;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PokeMain {
    public static List<PokemonInfo> fetchPokemonData() {
        ObjectMapper om = new ObjectMapper();
        URI uri;
        List<PokemonInfo> result = new ArrayList<>();

        try {
            uri = new URI("https://pokeapi.co/api/v2/pokemon");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        int count = 0;
        while (uri != null && count < 20) {
            try {
                URL url = uri.toURL();
                PokemonList list = om.readValue(url, PokemonList.class);

                for (Pokemon poke : list.results) {
                    PokemonDetails details = getPokemonDetails(poke.url, om);
                    if (details != null) {
                        result.add(new PokemonInfo(
                                poke.name,
                                poke.url,
                                details.height,
                                details.weight
                        ));
                        count++;
                    }
                }

                uri = (list.next != null && !list.next.isEmpty()) ? new URI(list.next) : null;
            } catch (Exception e) {
                break;
            }
        }

        return result;
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