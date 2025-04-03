package com.ghostreborn.akira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
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
                TextView animeName = findViewById(R.id.animeName);
                TextView animeDescription = findViewById(R.id.animeDescription);
                ImageView animeImage = findViewById(R.id.animeImage);
                ImageView animeBanner = findViewById(R.id.animeBanner);
                Button watchButton = findViewById(R.id.watch_button);
                TextView moreButton = findViewById(R.id.more_button);

                moreButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, EpisodesActivity.class);
                    assert animeDetails != null;
                    intent.putExtra("ANIME_ID", animeID);
                    intent.putStringArrayListExtra("EPISODE_LIST", animeDetails.getAnimeEpisodes());
                    startActivity(intent);
                });

                watchButton.setOnClickListener(v -> executor.execute(() -> {
                    assert animeDetails != null;
                    String url = new AllAnimeStream().serverUrls(animeID, animeDetails.getAnimeEpisodes().get(0)).get(0);
                    mainHandler.post(() -> {
                        Intent intent = new Intent(this, PlayActivity.class);
                        intent.putExtra("SERVER_URL", url);
                        startActivity(intent);
                    });
                }));

                assert animeDetails != null;
                EpisodeAdapter adapter = new EpisodeAdapter(this, animeID, animeDetails.getAnimeEpisodes(), 5);
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