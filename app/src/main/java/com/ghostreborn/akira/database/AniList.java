package com.ghostreborn.akira.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "aniList")
public class AniList {

    @PrimaryKey @NonNull
    public String malID;

    @ColumnInfo(name = "allAnimeID")
    public String allAnimeID;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "progress")
    public String progress;

    public AniList(@NonNull String malID, String allAnimeID, String title, String progress){
        this.malID = malID;
        this.allAnimeID = allAnimeID;
        this.title = title;
        this.progress = progress;
    }

}
