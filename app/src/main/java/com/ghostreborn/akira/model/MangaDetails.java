package com.ghostreborn.akira.model;

import java.util.ArrayList;

public class MangaDetails {

    private final String mangaName;
    private final String mangaDescription;
    private final String mangaBanner;
    private final String mangaThumbnail;
    private final ArrayList<String> mangaChapters;

    public MangaDetails(
            String mangaName,
            String mangaDescription,
            String mangaBanner,
            String mangaThumbnail,
            ArrayList<String> mangaChapters
    ){
        this.mangaName = mangaName;
        this.mangaDescription = mangaDescription;
        this.mangaBanner = mangaBanner;
        this.mangaThumbnail = mangaThumbnail;
        this.mangaChapters = mangaChapters;
    }

    public String getMangaName() {
        return mangaName;
    }

    public String getMangaDescription() {
        return mangaDescription;
    }

    public String getMangaBanner() {
        return mangaBanner;
    }

    public String getMangaThumbnail() {
        return mangaThumbnail;
    }

    public ArrayList<String> getMangaChapters() {
        return mangaChapters;
    }
}
