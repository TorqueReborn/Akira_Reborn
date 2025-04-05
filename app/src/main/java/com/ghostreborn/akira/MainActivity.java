package com.ghostreborn.akira;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.ghostreborn.akira.fragment.PopularAnimeFragment;
import com.ghostreborn.akira.fragment.PopularMangaFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private int previousSelectedIndex = 0;
    private long lastFragmentLoadTime = 0;
    private static final int LOAD_FRAGMENT_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPref = getSharedPreferences("AKIRA", Context.MODE_PRIVATE);
        boolean tokenSaved = sharedPref.getBoolean("TOKEN_SAVED", false);

        if (!tokenSaved){
            String queryUrl = "https://anilist.co/api/v2/oauth/authorize?client_id="+ 25543+"&redirect_uri="+"akira://ghostreborn.in"+"&response_type=code";
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(queryUrl)));
        } else {
            Log.e("TAG", sharedPref.getString("ANILIST_TOKEN", ""));
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);

    }

    private boolean loadFragment(Fragment fragment) {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastFragmentLoadTime >= LOAD_FRAGMENT_DELAY && fragment != null) {
            lastFragmentLoadTime = currentTime;

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int selectedItemId = item.getItemId();
        int selectedIndex = -1;

        if (selectedItemId == R.id.bottom_navigation_anime) {
            selectedIndex = 0;
        } else if (selectedItemId == R.id.bottom_navigation_manga) {
            selectedIndex = 1;
        }

        if (selectedIndex != -1 && selectedIndex != previousSelectedIndex) {
            Fragment fragment;

            if (selectedItemId == R.id.bottom_navigation_anime) {
                fragment = new PopularAnimeFragment();
            } else {
                fragment = new PopularMangaFragment();
            }

            if (loadFragment(fragment)) {
                previousSelectedIndex = selectedIndex;
                return true;
            }
        }

        return true;
    }
}