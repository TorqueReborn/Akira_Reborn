package com.ghostreborn.akira.fragment;

import android.content.Intent;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.adapter.AnimeAdapter;
import com.ghostreborn.akira.allAnime.AllAnimeDetails;
import com.ghostreborn.akira.allAnime.AllAnimeSearchMal;
import com.ghostreborn.akira.anilist.AniListAuthorize;
import com.ghostreborn.akira.database.AniList;
import com.ghostreborn.akira.database.AniListDao;
import com.ghostreborn.akira.database.AniListDatabase;
import com.ghostreborn.akira.model.Anime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AnilistFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_anilist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView loginButton = view.findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> {
            String queryUrl = "https://anilist.co/api/v2/oauth/authorize?client_id=25543&redirect_uri=akira://ghostreborn.in&response_type=code";
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(queryUrl)));
        });

        RecyclerView aniListRecycler = view.findViewById(R.id.anilist_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        aniListRecycler.setLayoutManager(layoutManager);

        List<Anime> detailedAnime = new ArrayList<>();
        AnimeAdapter adapter = new AnimeAdapter(requireContext(), detailedAnime);
        aniListRecycler.setAdapter(adapter);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            AniListDatabase db = AniListDatabase.getDatabase(getActivity());
            AniListDao aniListDao = db.aniListDao();
            List<AniList> aniLists = aniListDao.getAll();
            for (AniList aniList : aniLists) {
                String id = new AllAnimeSearchMal().getAllAnimeId(aniList.title, aniList.malID);
                Anime anime = new AllAnimeDetails().animeDetails(id);
                mainHandler.post(() -> {
                    detailedAnime.add(anime);
                    adapter.notifyItemInserted(detailedAnime.size() - 1);
                });
            }
        });

        parseAccessToken();

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
