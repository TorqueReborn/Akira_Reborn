package com.ghostreborn.akira.ui;

import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.adapter.EpisodeAdapter;

import java.util.ArrayList;

public class EpisodesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_episodes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String animeID = getIntent().getStringExtra("ANIME_ID");
        String aniListId = getIntent().getStringExtra("ANILIST_ID");
        ArrayList<String> episodes = getIntent().getStringArrayListExtra("EPISODE_LIST");

        // Reverse the array
        ArrayList<String> reversedEpisodes = new ArrayList<>();
        assert episodes != null;
        for (int i = episodes.size() - 1; i >= 0; i--) {
            reversedEpisodes.add(episodes.get(i));
        }

        RecyclerView episodeRecycler = findViewById(R.id.episode_recycler);
        GridLayoutManager manager = new GridLayoutManager(this, 5);
        episodeRecycler.setLayoutManager(manager);

        ProgressBar loadingProgress = findViewById(R.id.loadingProgress);

        EpisodeAdapter adapter = new EpisodeAdapter(this, animeID, aniListId, reversedEpisodes, episodes.size(), loadingProgress);
        episodeRecycler.setAdapter(adapter);

    }
}