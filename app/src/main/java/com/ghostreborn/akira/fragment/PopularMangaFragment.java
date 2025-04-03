package com.ghostreborn.akira.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.adapter.MangaAdapter;
import com.ghostreborn.akira.allManga.AllMangaDetails;
import com.ghostreborn.akira.allManga.AllMangaQueryPopular;
import com.ghostreborn.akira.model.Manga;
import com.ghostreborn.akira.ui.NoNetworkActivity;
import com.ghostreborn.akira.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PopularMangaFragment extends Fragment {

    private RecyclerView mangaRecycler;

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

    }

    private void getManga() {
        ExecutorService executor = Executors.newCachedThreadPool();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Manga> manga = new AllMangaQueryPopular().queryPopular();

            mainHandler.post(() -> {
                List<Manga> detailedManga = new ArrayList<>();
                MangaAdapter adapter = new MangaAdapter(requireContext(), detailedManga);
                mangaRecycler.setAdapter(adapter);

                for (Manga currentManga : manga) {
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
    }

}
