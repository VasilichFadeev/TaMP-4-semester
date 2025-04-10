package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.pokemons.Pokemon;
import org.example.pokemons.PokemonList;
import org.example.pokemons.PokemonDetails;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class PokeMain {
    public static void main(String[] args) {
        ObjectMapper om = new ObjectMapper();
        URI uri = null;
        try {
            uri = new URI("https://pokeapi.co/api/v2/pokemon");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        while (uri != null) {
            int count = 0;
            try {
                URL url = uri.toURL();
                PokemonList list = om.readValue(url, PokemonList.class);
                for (Pokemon poke : list.results) {
                    System.out.println("Name: " + poke.name);
                    System.out.println("URL: " + poke.url);
                    PokemonDetails details = getPokemonDetails(poke.url, om);
                    if (details != null) {
                        System.out.println("Height: " + details.height);
                        System.out.println("Weight: " + details.weight + "\n");
                    }
                    count++;
                    if (count == 5) {
                        return;
                    }
                }
                uri = (list.next != null && !list.next.isEmpty()) ? new URI(list.next) : null;
            } catch (Exception e) {
                System.out.println("JsonProcessingException found: " + e.getMessage() + ", " + e);
                throw new RuntimeException(e);
            }
        }
    }

    private static PokemonDetails getPokemonDetails(String url, ObjectMapper om) {
        try {
            URL pokemonUrl = new URL(url);
            return om.readValue(pokemonUrl, PokemonDetails.class);
        } catch (Exception e) {
            System.out.println("Error fetching details for " + url + ": " + e.getMessage());
            return null;
        }
    }
}