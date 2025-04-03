package com.ghostreborn.akira.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.adapter.AnimeAdapter;
import com.ghostreborn.akira.allAnime.AllAnimeDetails;
import com.ghostreborn.akira.allAnime.AllAnimeQueryPopular;
import com.ghostreborn.akira.allAnime.AllAnimeSearch;
import com.ghostreborn.akira.model.Anime;
import com.ghostreborn.akira.ui.NoNetworkActivity;
import com.ghostreborn.akira.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PopularAnimeFragment extends Fragment {

    private RecyclerView animeRecycler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_popular_anime, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (MiscUtils.isNetworkConnected(requireContext())) {
            getAnime();
        } else {
            startActivity(new Intent(requireContext(), NoNetworkActivity.class));
            requireActivity().finish();
        }

        animeRecycler = view.findViewById(R.id.anime_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        animeRecycler.setLayoutManager(layoutManager);

        SearchView searchView = view.findViewById(R.id.anime_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ExecutorService executor = Executors.newCachedThreadPool();
                Handler mainHandler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    List<Anime> anime = new AllAnimeSearch().search(query);

                    mainHandler.post(() -> {
                        List<Anime> detailedAnime = new ArrayList<>();
                        AnimeAdapter adapter = new AnimeAdapter(requireContext(), detailedAnime);
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
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }

    private void getAnime() {
        ExecutorService executor = Executors.newCachedThreadPool();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Anime> anime = new AllAnimeQueryPopular().queryPopular();

            mainHandler.post(() -> {
                List<Anime> detailedAnime = new ArrayList<>();
                AnimeAdapter adapter = new AnimeAdapter(requireContext(), detailedAnime);
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
