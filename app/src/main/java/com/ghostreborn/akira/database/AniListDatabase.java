package com.ghostreborn.akira.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AniList.class}, version = 1)
public abstract class AniListDatabase extends RoomDatabase {

    public abstract AniListDao aniListDao();

    private static volatile AniListDatabase INSTANCE;

    public static AniListDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (AniListDatabase.class) {
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AniListDatabase.class, "akira_database")
                            .build();

                }
            }
        }
        return INSTANCE;
    }

}
