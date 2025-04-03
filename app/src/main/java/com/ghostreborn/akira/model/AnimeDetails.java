package com.ghostreborn.akira.model;

import java.util.ArrayList;

public class AnimeDetails {

    private final String animeName;
    private final String animeDescription;
    private final String animeBanner;
    private final String animeThumbnail;
    private final String animePrequel;
    private final String animeSequel;
    private final ArrayList<String> animeEpisodes;

    public AnimeDetails(
            String animeName,
            String animeDescription,
            String animeBanner,
            String animeThumbnail,
            String animePrequel,
            String animeSequel,
            ArrayList<String> animeEpisodes
    ){
        this.animeName = animeName;
        this.animeDescription = animeDescription;
        this.animeBanner = animeBanner;
        this.animeThumbnail = animeThumbnail;
        this.animePrequel = animePrequel;
        this.animeSequel = animeSequel;
        this.animeEpisodes = animeEpisodes;
    }

    public String getAnimeName() {
        return animeName;
    }

    public String getAnimeDescription() {
        return animeDescription;
    }

    public String getAnimeBanner() {
        return animeBanner;
    }

    public String getAnimeThumbnail() {
        return animeThumbnail;
    }

    public String getAnimePrequel() {
        return animePrequel;
    }

    public String getAnimeSequel() {
        return animeSequel;
    }

    public ArrayList<String> getAnimeEpisodes() {
        return animeEpisodes;
    }
}
