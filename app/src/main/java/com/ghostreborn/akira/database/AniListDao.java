package com.ghostreborn.akira.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AniListDao {

    @Query("SELECT allAnimeID FROM aniList")
    List<String> getAllAnimeIDs();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<AniList> aniLists);

    @Update
    void update(AniList aniList);
}
