package com.ghostreborn.akira.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.ghostreborn.akira.R;

import java.util.ArrayList;

public class ReadActivity extends AppCompatActivity {

    private int currentIndex;
    private String currentPageInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_read);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ArrayList<String> thumbnails = getIntent().getStringArrayListExtra("MANGA_THUMBNAILS");
        if (thumbnails == null) {
            finish();
        }
        assert thumbnails != null;

        currentIndex = 0;
        currentPageInfo = "Page " + (currentIndex + 1) + "/" + thumbnails.size();

        ImageView mangaThumbnail = findViewById(R.id.manga_thumbnail);
        TextView pageInfo = findViewById(R.id.page_text);

        pageInfo.setText(currentPageInfo);

        Glide.with(this)
                .load(thumbnails.get(0))
                .into(mangaThumbnail);

        TextView leftButton = findViewById(R.id.leftButton);
        TextView rightButton = findViewById(R.id.rightButton);

        leftButton.setOnClickListener(v -> decreasePage(thumbnails, mangaThumbnail, pageInfo));
        rightButton.setOnClickListener(v -> increasePage(thumbnails, mangaThumbnail, pageInfo));

    }

    private void increasePage(ArrayList<String> thumbnails, ImageView mangaThumbnail, TextView pageInfo) {
        if (currentIndex == thumbnails.size() - 1) {
            return;
        }
        currentIndex++;
        currentPageInfo = "Page " + (currentIndex + 1) + "/" + thumbnails.size();
        pageInfo.setText(currentPageInfo);
        Glide.with(this)
                .load(thumbnails.get(currentIndex))
                .into(mangaThumbnail);
    }

    private void decreasePage(ArrayList<String> thumbnails, ImageView mangaThumbnail, TextView pageInfo) {
        if (currentIndex == 0) {
            return;
        }
        currentIndex--;
        currentPageInfo = "Page " + (currentIndex + 1) + "/" + thumbnails.size();
        pageInfo.setText(currentPageInfo);
        Glide.with(this)
                .load(thumbnails.get(currentIndex))
                .into(mangaThumbnail);
    }
}