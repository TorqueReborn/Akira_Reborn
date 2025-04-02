package com.ghostreborn.akira.model;

import java.util.ArrayList;

public class AnimeDetails {

    private final String animeName;
    private final String animeDescription;
    private final String animeBanner;
    private final String animeThumbnail;
    private final ArrayList<String> animeEpisodes;

    public AnimeDetails(
            String animeName,
            String animeDescription,
            String animeBanner,
            String animeThumbnail,
            ArrayList<String> animeEpisodes
    ){
        this.animeName = animeName;
        this.animeDescription = animeDescription;
        this.animeBanner = animeBanner;
        this.animeThumbnail = animeThumbnail;
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

    public ArrayList<String> getAnimeEpisodes() {
        return animeEpisodes;
    }
}
