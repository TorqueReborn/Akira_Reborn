package com.ghostreborn.akira.ui;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.ghostreborn.akira.R;

import java.util.ArrayList;

public class ReadActivity extends AppCompatActivity {

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
        if(thumbnails == null){
            finish();
        }

        assert thumbnails != null;

        ImageView mangaThumbnail = findViewById(R.id.manga_thumbnail);

        Glide.with(this)
                .load(thumbnails.get(0))
                .into(mangaThumbnail);

    }
}