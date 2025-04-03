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
import com.ghostreborn.akira.R;
import com.ghostreborn.akira.model.Manga;
import com.ghostreborn.akira.ui.MangaDetailsActivity;

import java.util.List;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.ViewHolder> {

    private final List<Manga> mangas;
    private final Context context;

    public MangaAdapter(Context context, List<Manga> mangas) {
        this.context = context;
        this.mangas = mangas;
    }

    @NonNull
    @Override
    public MangaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.manga_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaAdapter.ViewHolder holder, int position) {
        Manga manga = mangas.get(position);
        Glide.with(context)
                .load(manga.getMangaImage())
                .into(holder.mangaImage);

        // Grey scale the image
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.35f);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        holder.mangaImage.setColorFilter(filter);

        holder.mangaName.setText(manga.getMangaName());
        holder.mangaEpisode.setText(manga.getMangaChapters());
        holder.mangaRating.setText(manga.getMangaRating());
        holder.mangaStatus.setText(manga.getMangaStatus());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MangaDetailsActivity.class);
            intent.putExtra("MANGA_ID", manga.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mangas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView mangaName;
        private final TextView mangaEpisode;
        private final TextView mangaRating;
        private final TextView mangaStatus;
        private final ImageView mangaImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mangaImage = itemView.findViewById(R.id.manga_image);
            mangaName = itemView.findViewById(R.id.manga_name);
            mangaEpisode = itemView.findViewById(R.id.manga_episodes);
            mangaRating = itemView.findViewById(R.id.manga_rating);
            mangaStatus = itemView.findViewById(R.id.manga_status);
        }
    }
}
