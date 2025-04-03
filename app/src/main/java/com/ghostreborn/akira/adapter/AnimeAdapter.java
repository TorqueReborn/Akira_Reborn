package com.ghostreborn.akira.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ghostreborn.akira.ui.AnimeDetailsActivity;
import com.ghostreborn.akira.R;
import com.ghostreborn.akira.model.Anime;

import java.util.List;

public class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.ViewHolder> {

    private final List<Anime> animes;
    private final Context context;

    public AnimeAdapter(Context context, List<Anime> animes){
        this.context = context;
        this.animes = animes;
    }

    @NonNull
    @Override
    public AnimeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.anime_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimeAdapter.ViewHolder holder, int position) {
        Anime anime = animes.get(position);
        Glide.with(context)
                .load(anime.getAnimeImage())
                .into(holder.animeImage);

        // Grey scale the image
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.35f);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        holder.animeImage.setColorFilter(filter);

        holder.animeName.setText(anime.getAnimeName());
        holder.animeEpisode.setText(anime.getAnimeEpisodes());
        holder.animeSeason.setText(anime.getAnimeRating());
        holder.animeStatus.setText(anime.getAnimeStatus());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimeDetailsActivity.class);
            intent.putExtra("ANIME_ID", anime.get_id());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return animes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView animeName;
        private final TextView animeEpisode;
        private final TextView animeSeason;
        private final TextView animeStatus;
        private final ImageView animeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            animeImage = itemView.findViewById(R.id.anime_image);
            animeName = itemView.findViewById(R.id.anime_name);
            animeEpisode = itemView.findViewById(R.id.anime_episodes);
            animeSeason = itemView.findViewById(R.id.anime_rating);
            animeStatus = itemView.findViewById(R.id.anime_status);
        }
    }
}
