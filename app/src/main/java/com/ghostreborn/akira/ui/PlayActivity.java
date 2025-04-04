package com.ghostreborn.akira.ui;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.aniskip.AniSkip;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayActivity extends AppCompatActivity {

    private final ArrayList<String> urls = new ArrayList<>();
    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private int currentIndex = 0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


//        urls = getIntent().getStringArrayListExtra("SERVER_URLS");
        urls.add("https://myanime.sharepoint.com/sites/chartlousty/_layouts/15/download.aspx?share=ETxO_oqjXidIrtr9bOw6h60BA5U7QU859SixO8VruwX5ZA");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        playerView = findViewById(R.id.player_view);
        progressBar = findViewById(R.id.progress_bar);

        initializePlayer();
        setFullscreen();
    }

    private void setFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        Uri videoUri = Uri.parse(urls.get(currentIndex));
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                executorService.execute(() -> {
                    Long endTime = new AniSkip().startSkip("58567", "1").get("endTime");
                    long skip = endTime!=null ? endTime : 0;
                    mainHandler.post(() -> player.seekTo(skip));
                });
                player.setTrackSelectionParameters(
                        player
                                .getTrackSelectionParameters()
                                .buildUpon()
                                .setOverrideForType(
                                        new TrackSelectionOverride(
                                                tracks.getGroups()
                                                        .get(0)
                                                        .getMediaTrackGroup(),
                                                0
                                        )
                                )
                                .build()
                );
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                currentIndex++;
                if (currentIndex < urls.size()) {
                    initializePlayer();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}