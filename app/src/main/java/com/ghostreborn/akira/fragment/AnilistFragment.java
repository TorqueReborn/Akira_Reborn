package com.ghostreborn.akira.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.ghostreborn.akira.R;

public class AnilistFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_anilist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView loginButton = view.findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> {
            String queryUrl = "https://anilist.co/api/v2/oauth/authorize?client_id=25543&redirect_uri=akira://ghostreborn.in&response_type=code";
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(queryUrl)));
        });

        parseAccessToken();

    }

    private void parseAccessToken(){
        FragmentActivity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String code = uri.getQueryParameter("code");
                    SharedPreferences preferences = activity.getSharedPreferences("AKIRA", Context.MODE_PRIVATE);
                    preferences.edit()
                            .putBoolean("TOKEN_SAVED", true)
                            .putString("ANILIST_CODE", code)
                            .apply();
                }
            }
        }
    }

}
