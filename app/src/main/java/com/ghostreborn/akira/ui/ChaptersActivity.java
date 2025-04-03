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

public class ChaptersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chapters);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String mangaID = getIntent().getStringExtra("MANGA_ID");
        ArrayList<String> chapters = getIntent().getStringArrayListExtra("CHAPTER_LIST");

        // Reverse the array
        ArrayList<String> reversedChapters = new ArrayList<>();
        assert chapters != null;
        for(int i = chapters.size()-1; i>=0; i--){
            reversedChapters.add(chapters.get(i));
        }

        RecyclerView episodeRecycler = findViewById(R.id.chapter_recycler);
        GridLayoutManager manager = new GridLayoutManager(this, 5);
        episodeRecycler.setLayoutManager(manager);

        ProgressBar loadingProgress = findViewById(R.id.loadingProgress);

        EpisodeAdapter adapter = new EpisodeAdapter(this, mangaID, reversedChapters, chapters.size(), loadingProgress);
        episodeRecycler.setAdapter(adapter);
    }
}