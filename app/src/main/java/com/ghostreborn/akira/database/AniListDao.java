package com.ghostreborn.akira.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AniListDao {
    @Query("SELECT * FROM aniList")
    List<AniList> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(AniList aniList);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<AniList> aniLists);

    @Update
    void update(AniList aniList);
}
