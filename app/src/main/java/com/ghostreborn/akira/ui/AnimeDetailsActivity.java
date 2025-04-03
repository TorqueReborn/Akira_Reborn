package com.ghostreborn.akira.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
                if (animeDetails == null){
                    finish();
                }
                TextView animeName = findViewById(R.id.animeName);
                TextView animeDescription = findViewById(R.id.animeDescription);
                ImageView animeImage = findViewById(R.id.animeImage);
                ImageView animeBanner = findViewById(R.id.animeBanner);

                assert animeDetails != null;
                EpisodeAdapter adapter = new EpisodeAdapter(this, animeDetails.getAnimeEpisodes(), 5);
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