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
import com.ghostreborn.akira.adapter.ChapterAdapter;
import com.ghostreborn.akira.allManga.AllMangaFullDetails;
import com.ghostreborn.akira.allManga.AllMangaRead;
import com.ghostreborn.akira.model.MangaDetails;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MangaDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manga_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String mangaID = getIntent().getStringExtra("MANGA_ID");

        ExecutorService executor = Executors.newCachedThreadPool();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        RecyclerView episodeRecycler = findViewById(R.id.chapterRecycler);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        episodeRecycler.setLayoutManager(manager);

        executor.execute(() -> {
            MangaDetails mangaDetails = new AllMangaFullDetails().fullDetails(mangaID);
            mainHandler.post(() -> {
                if (mangaDetails == null) {
                    finish();
                }

                assert mangaDetails != null;

                TextView mangaName = findViewById(R.id.mangaName);
                TextView mangaDescription = findViewById(R.id.mangaDescription);
                ImageView mangaImage = findViewById(R.id.mangaImage);
                ImageView mangaBanner = findViewById(R.id.mangaBanner);
                Button readButton = findViewById(R.id.read_button);
                TextView moreButton = findViewById(R.id.more_button);
                ProgressBar loadingProgress = findViewById(R.id.loading_progress);

                moreButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ChaptersActivity.class);
                    intent.putExtra("MANGA_ID", mangaID);
                    intent.putStringArrayListExtra("CHAPTER_LIST", mangaDetails.getMangaChapters());
                    startActivity(intent);
                });

                readButton.setOnClickListener(v -> {
                    loadingProgress.setVisibility(View.VISIBLE);
                    executor.execute(() -> {
                        ArrayList<String> thumbnails = new AllMangaRead().getChapters(mangaID, mangaDetails.getMangaChapters().get(mangaDetails.getMangaChapters().size() - 1));
                        mainHandler.post(() -> {
                            Intent intent = new Intent(this, ReadActivity.class);
                            intent.putStringArrayListExtra("MANGA_THUMBNAILS", thumbnails);
                            startActivity(intent);
                            loadingProgress.setVisibility(View.GONE);
                        });
                    });
                });

                ChapterAdapter adapter = new ChapterAdapter(this, mangaID, mangaDetails.getMangaChapters(), 5, loadingProgress);
                episodeRecycler.setAdapter(adapter);

                mangaName.setText(mangaDetails.getMangaName());
                mangaDescription.setText(mangaDetails.getMangaDescription());

                Glide.with(this)
                        .load(mangaDetails.getMangaThumbnail())
                        .into(mangaImage);
                Glide.with(this)
                        .load(mangaDetails.getMangaBanner())
                        .into(mangaBanner);
            });
        });

    }
}