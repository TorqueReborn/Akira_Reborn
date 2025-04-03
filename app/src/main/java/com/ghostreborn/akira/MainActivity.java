package com.ghostreborn.akira;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.adapter.AnimeAdapter;
import com.ghostreborn.akira.allAnime.AllAnimeDetails;
import com.ghostreborn.akira.allAnime.AllAnimeQueryPopular;
import com.ghostreborn.akira.model.Anime;
import com.ghostreborn.akira.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView animeRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(MiscUtils.isNetworkConnected(this)){
            getAnime();
        } else{
            startActivity(new Intent(this, NoNetworkActivity.class));
            finish();
        }

        animeRecycler = findViewById(R.id.anime_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        animeRecycler.setLayoutManager(layoutManager);

    }

    private void getAnime(){
        ExecutorService executor = Executors.newCachedThreadPool();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Anime> anime = new AllAnimeQueryPopular().queryPopular();

            mainHandler.post(() -> {
                List<Anime> detailedAnime = new ArrayList<>();
                AnimeAdapter adapter = new AnimeAdapter(this, detailedAnime);
                animeRecycler.setAdapter(adapter);

                for (Anime currentAnime : anime) {
                    executor.execute(() -> {
                        Anime detailed = new AllAnimeDetails().animeDetails(currentAnime);
                        mainHandler.post(() -> {
                            detailedAnime.add(detailed);
                            adapter.notifyItemInserted(detailedAnime.size() - 1);
                        });
                    });
                }
            });
        });
    }
}