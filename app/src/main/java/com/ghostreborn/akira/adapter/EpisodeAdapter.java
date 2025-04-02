package com.ghostreborn.akira.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ghostreborn.akira.R;

import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final List<String> episodes;
    private final int limit;
    private final Context context;

    public EpisodeAdapter(Context context, List<String> episodes, int limit) {
        this.context = context;
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