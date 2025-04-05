package com.ghostreborn.akira.ui;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.ghostreborn.akira.R;
import com.ghostreborn.akira.aniskip.AniSkip;
import com.ghostreborn.akira.utils.HighlightedProgressbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayActivity extends AppCompatActivity {

    private final List<Pair<Long, Long>> highlightIntervals = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Timer timer;
    private String aniListId = "";
    private String episodeNumber = "";
    private int currentIndex = 0;
    private ArrayList<String> urls = new ArrayList<>();
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView skipButton;
    private HighlightedProgressbar highlightedProgressbar;

    public void startRecurringTask() {
        if (timer != null) {
            stopRecurringTask();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkDuration(highlightIntervals);
            }
        }, 1, TimeUnit.SECONDS.toMillis(2));
    }

    public void stopRecurringTask() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void checkDuration(List<Pair<Long, Long>> highlightIntervals) {
        if (highlightIntervals != null) {
            PlayActivity activity = this;
            activity.runOnUiThread(() -> {
                long playerDuration = player.getCurrentPosition();
                for (int i = 0; i < highlightIntervals.size(); i++) {
                    if (playerDuration >= highlightIntervals.get(i).first && playerDuration <= highlightIntervals.get(i).second) {
                        skipButton.setVisibility(View.VISIBLE);
                        int finalI = i;
                        skipButton.setOnClickListener(v -> player.seekTo(highlightIntervals.get(finalI).second));
                    } else {
                        skipButton.setVisibility(View.GONE);
                    }
                }
            });
        }
    }


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

        highlightedProgressbar = findViewById(R.id.highlighted_progress);
        skipButton = findViewById(R.id.skip_button);

        urls = getIntent().getStringArrayListExtra("SERVER_URLS");
        episodeNumber = getIntent().getStringExtra("EPISODE_NUMBER");
        aniListId = getIntent().getStringExtra("ANILIST_ID");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        playerView = findViewById(R.id.player_view);

        initializePlayer();
        setFullscreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecurringTask();
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

        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) this::animateHighlightedProgressBar);

        player.addListener(new Player.Listener() {

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                executorService.execute(() -> {
                    if (episodeNumber != null && aniListId != null) {
                        Map<String, Long> skip = new AniSkip().startEndSkip(aniListId, episodeNumber);

                        List<Pair<Long, Long>> timePairs = new ArrayList<>();
                        highlightIntervals.clear();

                        Long opStart = skip.get("OP_startTime");
                        Long opEnd = skip.get("OP_endTime");
                        if (opStart != null && opEnd != null) {
                            timePairs.add(new Pair<>(opStart, opEnd));
                        }

                        Long edStart = skip.get("ED_startTime");
                        Long edEnd = skip.get("ED_endTime");
                        if (edStart != null && edEnd != null) {
                            timePairs.add(new Pair<>(edStart, edEnd));
                        }

                        highlightIntervals.addAll(timePairs);
                        startRecurringTask();
                        mainHandler.post(() -> highlightedProgressbar.setDurationAndHighlightIntervals(player.getDuration(), highlightIntervals));
                    }
                });
                TrackGroup mediaTrackGroup = tracks.getGroups().get(0).getMediaTrackGroup();
                player.setTrackSelectionParameters(
                        player.getTrackSelectionParameters()
                                .buildUpon()
                                .setOverrideForType(new TrackSelectionOverride(mediaTrackGroup, 0))
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

    private void animateHighlightedProgressBar(int visibility) {
        float targetTranslationY = 0;
        if (visibility == View.GONE) {
            targetTranslationY = playerView.findViewById(R.id.exo_progress).getHeight();
        }

        highlightedProgressbar.animate()
                .translationY(targetTranslationY)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
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