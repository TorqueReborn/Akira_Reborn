package com.ghostreborn.akira.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import androidx.fragment.app.FragmentActivity;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.allAnime.AllAnimeFullDetails;

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

        parseAccessToken();
    }

    private void connectAllAnime() {
        String rawJSON = new AllAnimeFullDetails().full("ReooPAxPMsHM4KPMY");
        mainHandler.post(() -> testText.setText(rawJSON));
    }

    private void parseAccessToken(){
        FragmentActivity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String code = uri.getQueryParameter("code");
                    SharedPreferences preferences = activity.getSharedPreferences("AKIRA", Context.MODE_PRIVATE);
                    preferences.edit()
                            .putBoolean("TOKEN_SAVED", true)
                            .putString("ANILIST_TOKEN", code)
                            .apply();
                }
            }
        }
    }


}
