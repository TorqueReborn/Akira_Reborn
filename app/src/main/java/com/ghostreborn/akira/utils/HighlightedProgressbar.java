package com.ghostreborn.akira.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ghostreborn.akira.R;

import java.util.ArrayList;
import java.util.List;

public class HighlightedProgressbar extends View {

    private final Paint highlightPaint;
    private long totalDurationMs = 0;
    private List<Pair<Long, Long>> highlightIntervals = new ArrayList<>();

    public HighlightedProgressbar(Context context) {
        this(context, null);
    }

    public HighlightedProgressbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HighlightedProgressbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        highlightPaint = new Paint();
        highlightPaint.setColor(ContextCompat.getColor(context, R.color.highlight_color));
        highlightPaint.setStyle(Paint.Style.FILL);
    }

    public void setDurationAndHighlightIntervals(long totalDurationMs, List<Pair<Long, Long>> highlightIntervals) {
        this.totalDurationMs = totalDurationMs;
        this.highlightIntervals = highlightIntervals;
        invalidate(); // Trigger redraw
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (totalDurationMs > 0 && highlightIntervals != null && !highlightIntervals.isEmpty()) {
            float progressBarWidth = getWidth();
            float progressBarHeight = getHeight(); // Or a fixed value if known

            for (Pair<Long, Long> interval : highlightIntervals) {
                long highlightStartMs = interval.first;
                long highlightEndMs = interval.second;

                if (highlightEndMs > highlightStartMs) {
                    float highlightStartPx = (float) highlightStartMs / totalDurationMs * progressBarWidth;
                    float highlightEndPx = (float) highlightEndMs / totalDurationMs * progressBarWidth;

                    canvas.drawRect(
                            highlightStartPx,
                            0f, // Top of the highlight
                            highlightEndPx,
                            progressBarHeight, // Bottom of the highlight
                            highlightPaint
                    );
                }
            }
        }
    }
}