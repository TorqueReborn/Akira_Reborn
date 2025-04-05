package com.ghostreborn.akira.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AniListDao {
    @Query("SELECT * FROM aniList")
    List<AniList> getAll();

    @Insert
    long insert(AniList aniList);
}
