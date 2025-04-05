package com.ghostreborn.akira.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.allAnime.AllAnimeDetailByIds;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestFragment extends Fragment {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView testText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        testText = view.findViewById(R.id.test_text);

        executorService.execute(this::connectAllAnime);
    }

    private void connectAllAnime() {

        AllAnimeDetailByIds detailByIds = new AllAnimeDetailByIds();

        ArrayList<String> ids = new ArrayList<>();
        ids.add("9NdrgcZjsp7HEJ5oK");
        ids.add("ReooPAxPMsHM4KPMY");

        String rawJSON = detailByIds.animeDetails(ids).get(0).getAnimeName();
        mainHandler.post(() -> testText.setText(rawJSON));
    }


}
