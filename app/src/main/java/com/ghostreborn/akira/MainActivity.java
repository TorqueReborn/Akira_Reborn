package com.ghostreborn.akira;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.ghostreborn.akira.fragment.AnilistFragment;
import com.ghostreborn.akira.fragment.PopularAnimeFragment;
import com.ghostreborn.akira.fragment.PopularMangaFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private static final int LOAD_FRAGMENT_DELAY = 1000;
    private int previousSelectedIndex = 0;
    private long lastFragmentLoadTime = 0;

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

        BottomNavigationView bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.bottom_navigation_anilist);
    }

    private boolean loadFragment(Fragment fragment) {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastFragmentLoadTime >= LOAD_FRAGMENT_DELAY && fragment != null) {
            lastFragmentLoadTime = currentTime;

            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, fragment).commit();
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
        } else if (selectedItemId == R.id.bottom_navigation_anilist) {
            selectedIndex = 2;
        }

        if (selectedIndex != -1 && selectedIndex != previousSelectedIndex) {
            Fragment fragment;

            if (selectedItemId == R.id.bottom_navigation_anime) {
                fragment = new PopularAnimeFragment();
            } else if (selectedItemId == R.id.bottom_navigation_manga) {
                fragment = new PopularMangaFragment();
            } else {
                fragment = new AnilistFragment();
            }

            if (loadFragment(fragment)) {
                previousSelectedIndex = selectedIndex;
                return true;
            }
        }

        return true;
    }
}