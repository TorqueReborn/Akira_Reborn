package com.ghostreborn.akira.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.allAnime.AllAnimeStream;
import com.ghostreborn.akira.ui.PlayActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final List<String> episodes;
    private final int limit;
    private final Context context;
    private final String id;
    private final ProgressBar loadingProgress;

    ExecutorService executor = Executors.newCachedThreadPool();
    Handler mainHandler = new Handler(Looper.getMainLooper());

    public EpisodeAdapter(Context context, String id, List<String> episodes, int limit, ProgressBar loadingProgress) {
        this.context = context;
        this.loadingProgress = loadingProgress;
        this.id = id;
        this.episodes = episodes;
        this.limit = limit;
    }

    @NonNull
    @Override
    public EpisodeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.episode_list, parent, false);
        return new EpisodeAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeAdapter.ViewHolder holder, int position) {
        if(episodes != null && position < episodes.size()){
            holder.episodeNumber.setText(episodes.get(position));
        }
        holder.itemView.setOnClickListener(v -> {
            loadingProgress.setVisibility(View.VISIBLE);
            executor.execute(() -> {
                assert episodes != null;
                ArrayList<String> urls = new AllAnimeStream().serverUrls(id, episodes.get(position));
                mainHandler.post(() -> {
                    Intent intent = new Intent(context, PlayActivity.class);
                    intent.putStringArrayListExtra("SERVER_URLS", urls);
                    context.startActivity(intent);
                    loadingProgress.setVisibility(View.GONE);
                });
            });
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(episodes.size(), limit);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView episodeNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeNumber = itemView.findViewById(R.id.episodeNumber);
        }
    }
}