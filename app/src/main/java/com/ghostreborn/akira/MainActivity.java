package com.ghostreborn.akira;

import android.os.Bundle;
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

    private int previousSelectedIndex = -1;

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

    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
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