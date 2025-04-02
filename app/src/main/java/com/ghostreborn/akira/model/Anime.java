package com.ghostreborn.akira.model;

public class Anime {

    private final String _id;
    private String animeName;
    private String animeImage;
    private String animeEpisodes;
    private String animeSeason;
    private String animeStatus;

    public Anime(
            String _id
    ){
        this._id = _id;
    }

    public String getAnimeName() {
        return animeName;
    }

    public String get_id() {
        return _id;
    }

    public String getAnimeImage() {
        return animeImage;
    }

    public String getAnimeEpisodes() {
        return animeEpisodes;
    }

    public String getAnimeSeason() {
        return animeSeason;
    }

    public String getAnimeStatus() {
        return animeStatus;
    }

    public void setAnimeName(String animeName) {
        this.animeName = animeName;
    }

    public void setAnimeImage(String animeImage) {
        this.animeImage = animeImage;
    }

    public void setAnimeEpisodes(String animeEpisodes) {
        this.animeEpisodes = animeEpisodes;
    }

    public void setAnimeSeason(String animeSeason) {
        this.animeSeason = animeSeason;
    }

    public void setAnimeStatus(String animeStatus) {
        this.animeStatus = animeStatus;
    }
}
