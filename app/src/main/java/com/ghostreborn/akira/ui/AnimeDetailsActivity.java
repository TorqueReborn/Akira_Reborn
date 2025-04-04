package com.ghostreborn.akira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ghostreborn.akira.R;
import com.ghostreborn.akira.adapter.EpisodeAdapter;
import com.ghostreborn.akira.allAnime.AllAnimeFullDetails;
import com.ghostreborn.akira.allAnime.AllAnimeStream;
import com.ghostreborn.akira.model.AnimeDetails;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnimeDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_anime_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String animeID = getIntent().getStringExtra("ANIME_ID");

        ExecutorService executor = Executors.newCachedThreadPool();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        RecyclerView episodeRecycler = findViewById(R.id.episodeRecycler);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        episodeRecycler.setLayoutManager(manager);

        executor.execute(() -> {
            AnimeDetails animeDetails = new AllAnimeFullDetails().fullDetails(animeID);
            mainHandler.post(() -> {
                if (animeDetails == null) {
                    finish();
                }

                assert animeDetails != null;

                TextView animeName = findViewById(R.id.animeName);
                TextView animeDescription = findViewById(R.id.animeDescription);
                ImageView animeImage = findViewById(R.id.animeImage);
                ImageView animeBanner = findViewById(R.id.animeBanner);
                Button watchButton = findViewById(R.id.watch_button);
                Button animePrequel = findViewById(R.id.animePrequel);
                Button animeSequel = findViewById(R.id.animeSequel);
                TextView moreButton = findViewById(R.id.more_button);
                ProgressBar loadingProgress = findViewById(R.id.loading_progress);

                if (!animeDetails.getAnimePrequel().isEmpty()) {
                    animePrequel.setVisibility(View.VISIBLE);
                    animePrequel.setOnClickListener(v -> {
                        Intent intent = new Intent(this, AnimeDetailsActivity.class);
                        intent.putExtra("ANIME_ID", animeDetails.getAnimePrequel());
                        startActivity(intent);
                    });
                }

                if (!animeDetails.getAnimeSequel().isEmpty()) {
                    animeSequel.setVisibility(View.VISIBLE);
                    animeSequel.setOnClickListener(v -> {
                        Intent intent = new Intent(this, AnimeDetailsActivity.class);
                        intent.putExtra("ANIME_ID", animeDetails.getAnimeSequel());
                        startActivity(intent);
                    });
                }

                moreButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, EpisodesActivity.class);
                    intent.putExtra("ANIME_ID", animeID);
                    intent.putExtra("ANILIST_ID", animeDetails.getAniListId());
                    intent.putStringArrayListExtra("EPISODE_LIST", animeDetails.getAnimeEpisodes());
                    startActivity(intent);
                });

                watchButton.setOnClickListener(v -> {
                    loadingProgress.setVisibility(View.VISIBLE);
                    executor.execute(() -> {
                        ArrayList<String> urls = new AllAnimeStream().serverUrls(animeID, animeDetails.getAnimeEpisodes().get(animeDetails.getAnimeEpisodes().size() - 1));
                        mainHandler.post(() -> {
                            Intent intent = new Intent(this, PlayActivity.class);
                            intent.putExtra("EPISODE_NUMBER", "1");
                            intent.putStringArrayListExtra("SERVER_URLS", urls);
                            startActivity(intent);
                            loadingProgress.setVisibility(View.GONE);
                        });
                    });
                });

                EpisodeAdapter adapter = new EpisodeAdapter(this, animeID, animeDetails.getAniListId(),animeDetails.getAnimeEpisodes(), 5, loadingProgress);
                episodeRecycler.setAdapter(adapter);

                animeName.setText(animeDetails.getAnimeName());
                animeDescription.setText(animeDetails.getAnimeDescription());

                Glide.with(this)
                        .load(animeDetails.getAnimeThumbnail())
                        .into(animeImage);
                Glide.with(this)
                        .load(animeDetails.getAnimeBanner())
                        .into(animeBanner);
            });
        });

    }
}