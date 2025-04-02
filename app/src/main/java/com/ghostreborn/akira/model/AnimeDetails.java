package com.ghostreborn.akira.model;

public class AnimeDetails {

    private final String animeBanner;
    private final String animeThumbnail;

    public AnimeDetails(
            String animeBanner,
            String animeThumbnail
    ){
        this.animeBanner = animeBanner;
        this.animeThumbnail = animeThumbnail;
    }

    public String getAnimeBanner() {
        return animeBanner;
    }

    public String getAnimeThumbnail() {
        return animeThumbnail;
    }
}
