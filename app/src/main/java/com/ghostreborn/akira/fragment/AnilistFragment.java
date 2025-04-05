package com.ghostreborn.akira.fragment;

import android.app.Activity;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.adapter.AnimeAdapter;
import com.ghostreborn.akira.allAnime.AllAnimeDetailByIds;
import com.ghostreborn.akira.anilist.AniListAuthorize;
import com.ghostreborn.akira.database.AniListDao;
import com.ghostreborn.akira.database.AniListDatabase;
import com.ghostreborn.akira.model.Anime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AnilistFragment extends Fragment {

    private View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_anilist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        parseAccessToken();
    }

    @Override
    public void onResume() {
        super.onResume();


        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences preferences = activity.getSharedPreferences("AKIRA", Context.MODE_PRIVATE);
            boolean isLoggedIn = preferences.getBoolean("ANILIST_LOGGED_IN", false);

            ConstraintLayout constraintLayout = mView.findViewById(R.id.anilist_login_constraint);
            if (!isLoggedIn) {
                constraintLayout.setVisibility(View.VISIBLE);
                TextView loginButton = mView.findViewById(R.id.login_button);
                loginButton.setOnClickListener(v -> {
                    String queryUrl = "https://anilist.co/api/v2/oauth/authorize?client_id=25543&redirect_uri=akira://ghostreborn.in&response_type=code";
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(queryUrl)));
                });
            }
        }else {
            RecyclerView aniListRecycler = mView.findViewById(R.id.anilist_recycler);
            GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
            aniListRecycler.setLayoutManager(layoutManager);

            Handler mainHandler = new Handler(Looper.getMainLooper());
            Executors.newSingleThreadExecutor().execute(() -> {
                AniListDatabase db = AniListDatabase.getDatabase(getActivity());
                AniListDao aniListDao = db.aniListDao();
                List<String> allAnimeIDs = aniListDao.getAllAnimeIDs();
                ArrayList<Anime> anime = new AllAnimeDetailByIds().animeDetails(allAnimeIDs);
                mainHandler.post(() -> {
                    AnimeAdapter adapter = new AnimeAdapter(requireContext(), anime);
                    aniListRecycler.setAdapter(adapter);
                });
            });
        }

    }

    private void parseAccessToken() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String code = uri.getQueryParameter("code");
                    Executors.newSingleThreadExecutor().execute(() -> new AniListAuthorize().getToken(code, getActivity()));
                }
            }
        }
    }

}
