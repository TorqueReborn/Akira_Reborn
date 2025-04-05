package com.ghostreborn.akira.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "aniList")
public class AniList {

    @PrimaryKey @NonNull
    public String malID;

    @ColumnInfo(name = "progress")
    public String progress;

    public AniList(@NonNull String malID, String progress){
        this.malID = malID;
        this.progress = progress;
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
