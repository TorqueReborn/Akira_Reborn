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

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    private final List<String> chapters;
    private final int limit;
    private final Context context;
    private final String id;
    private final ProgressBar loadingProgress;

    ExecutorService executor = Executors.newCachedThreadPool();
    Handler mainHandler = new Handler(Looper.getMainLooper());

    public ChapterAdapter(Context context, String id, List<String> episodes, int limit, ProgressBar loadingProgress) {
        this.context = context;
        this.loadingProgress = loadingProgress;
        this.id = id;
        this.chapters = episodes;
        this.limit = limit;
    }

    @NonNull
    @Override
    public ChapterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.chapter_list, parent, false);
        return new ChapterAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterAdapter.ViewHolder holder, int position) {
        if(chapters != null && position < chapters.size()){
            holder.chapterNumber.setText(chapters.get(position));
        }
        holder.itemView.setOnClickListener(v -> {
            loadingProgress.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(chapters.size(), limit);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView chapterNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterNumber = itemView.findViewById(R.id.episodeNumber);
        }
    }
}