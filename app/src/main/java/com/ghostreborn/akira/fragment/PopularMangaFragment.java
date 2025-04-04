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
import com.ghostreborn.akira.adapter.MangaAdapter;
import com.ghostreborn.akira.allManga.AllMangaDetailByIds;
import com.ghostreborn.akira.allManga.AllMangaDetails;
import com.ghostreborn.akira.allManga.AllMangaQueryPopular;
import com.ghostreborn.akira.allManga.AllMangaSearch;
import com.ghostreborn.akira.model.Manga;
import com.ghostreborn.akira.ui.NoNetworkActivity;
import com.ghostreborn.akira.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PopularMangaFragment extends Fragment {

    private RecyclerView mangaRecycler;
    private SearchView searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_popular_manga, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (MiscUtils.isNetworkConnected(requireContext())) {
            getManga();
        } else {
            startActivity(new Intent(requireContext(), NoNetworkActivity.class));
            requireActivity().finish();
        }

        mangaRecycler = view.findViewById(R.id.manga_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        mangaRecycler.setLayoutManager(layoutManager);

        searchView = view.findViewById(R.id.manga_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ExecutorService executor = Executors.newCachedThreadPool();
                Handler mainHandler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    List<Manga> mangas = new AllMangaSearch().search(query);

                    mainHandler.post(() -> {
                        List<Manga> detailedManga = new ArrayList<>();
                        MangaAdapter adapter = new MangaAdapter(requireContext(), detailedManga);
                        mangaRecycler.setAdapter(adapter);

                        for (Manga currentManga : mangas) {
                            executor.execute(() -> {
                                Manga detailed = new AllMangaDetails().mangaDetails(currentManga);
                                mainHandler.post(() -> {
                                    detailedManga.add(detailed);
                                    adapter.notifyItemInserted(detailedManga.size() - 1);
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

    @Override
    public void onPause() {
        super.onPause();
        if (searchView != null) {
            searchView.clearFocus();
        }
    }

    private void getManga() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> allAnimeIDs = new AllMangaQueryPopular().queryPopular();
            ArrayList<Manga> manga = new AllMangaDetailByIds().mangaDetails(allAnimeIDs);
            mainHandler.post(() -> {
                MangaAdapter adapter = new MangaAdapter(requireContext(), manga);
                mangaRecycler.setAdapter(adapter);
            });
        });
    }

}
