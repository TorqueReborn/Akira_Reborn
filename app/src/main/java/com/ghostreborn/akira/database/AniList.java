package com.ghostreborn.akira.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "aniList")
public class AniList {

    @PrimaryKey @NonNull
    public String malID;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "progress")
    public String progress;

    public AniList(@NonNull String malID, String title,String progress){
        this.malID = malID;
        this.title = title;
        this.progress = progress;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @NonNull
    public String getMalID() {
        return malID;
    }

    public void setMalID(@NonNull String malID) {
        this.malID = malID;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }
}
