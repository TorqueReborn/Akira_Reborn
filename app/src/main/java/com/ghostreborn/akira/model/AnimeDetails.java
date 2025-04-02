package com.ghostreborn.akira.model;

public class AnimeDetails {

    private final String animeName;
    private final String animeDescription;
    private final String animeBanner;
    private final String animeThumbnail;

    public AnimeDetails(
            String animeName,
            String animeDescription,
            String animeBanner,
            String animeThumbnail
    ){
        this.animeName = animeName;
        this.animeDescription = animeDescription;
        this.animeBanner = animeBanner;
        this.animeThumbnail = animeThumbnail;
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
}
