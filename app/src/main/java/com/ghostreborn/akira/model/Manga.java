package com.ghostreborn.akira.model;

public class Manga {

    private final String id;
    private String mangaName;
    private String mangaImage;
    private String mangaChapters;
    private String mangaRating;
    private String mangaStatus;

    public Manga(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getMangaImage() {
        return mangaImage;
    }

    public String getMangaName() {
        return mangaName;
    }

    public String getMangaChapters() {
        return mangaChapters;
    }

    public String getMangaRating() {
        return mangaRating;
    }

    public String getMangaStatus() {
        return mangaStatus;
    }

    public void setMangaChapters(String mangaChapters) {
        this.mangaChapters = mangaChapters;
    }

    public void setMangaRating(String mangaRating) {
        this.mangaRating = mangaRating;
    }

    public void setMangaStatus(String mangaStatus) {
        this.mangaStatus = mangaStatus;
    }

    public void setMangaImage(String mangaImage) {
        this.mangaImage = mangaImage;
    }

    public void setMangaName(String mangaName) {
        this.mangaName = mangaName;
    }
}
