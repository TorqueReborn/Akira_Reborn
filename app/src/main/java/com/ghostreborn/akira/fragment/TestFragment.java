package com.ghostreborn.akira.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.database.AniList;
import com.ghostreborn.akira.database.AniListDao;
import com.ghostreborn.akira.database.AniListDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestFragment extends Fragment {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView testText;

    private AniListDatabase db;
    private AniListDao aniListDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        testText = view.findViewById(R.id.test_text);

        db = AniListDatabase.getDatabase(getContext());
        aniListDao = db.aniListDao();

        executorService.execute(this::connectAllAnime);
    }

    private void connectAllAnime() {
//        AniList anilist = new AniList("58939", "7");
//        long inserted = aniListDao.insert(anilist);
        List<AniList> aniLists = aniListDao.getAll();
        for (AniList aniList : aniLists) {
            Log.e("TAG", aniList.malID + " " + aniList.progress);
        }
        mainHandler.post(() -> {
//            Toast.makeText(getContext(), inserted + "", Toast.LENGTH_SHORT).show();
        });
    }


}
