package com.ghostreborn.akira.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private final PointF lastTouch = new PointF();
    private final Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private int mode = NONE;
    private float oldDist = 1f;
    private final float[] matrixValues = new float[9];
    private int viewWidth, viewHeight;
    private float originalWidth, originalHeight;

    private boolean isClick;
    private int touchSlop;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(matrix);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setOnTouchListener((v, event) -> {
            handleTouch(event);
            scaleGestureDetector.onTouchEvent(event);
            if (isClick && event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                viewWidth = getWidth();
                viewHeight = getHeight();
                if (getDrawable() != null) {
                    originalWidth = getDrawable().getIntrinsicWidth();
                    originalHeight = getDrawable().getIntrinsicHeight();
                    fitImageToScreen();
                }
            }
        });
    }

    private void fitImageToScreen() {
        if (originalWidth > 0 && originalHeight > 0 && viewWidth > 0 && viewHeight > 0) {
            float scaleX = (float) viewWidth / originalWidth;
            float scaleY = (float) viewHeight / originalHeight;
            float scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);

            float dx = (viewWidth - originalWidth * scale) / 2;
            float dy = (viewHeight - originalHeight * scale) / 2;
            matrix.postTranslate(dx, dy);

            setImageMatrix(matrix);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void handleTouch(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                lastTouch.set(event.getX(), event.getY());
                mode = DRAG;
                isClick = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(lastTouch, event);
                    mode = ZOOM;
                    isClick = false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - lastTouch.x, event.getY() - lastTouch.y);
                    fixTranslation();
                    if (isClick && (Math.abs(event.getX() - lastTouch.x) > touchSlop || Math.abs(event.getY() - lastTouch.y) > touchSlop)) {
                        isClick = false;
                    }
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        matrix.postScale(scale, scale, lastTouch.x, lastTouch.y);
                        fixScaleAndTranslation();
                    }
                }
                break;
        }
        setImageMatrix(matrix);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private void fixTranslation() {
        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float drawableWidth = getDrawable().getIntrinsicWidth() * matrixValues[Matrix.MSCALE_X];
        float drawableHeight = getDrawable().getIntrinsicHeight() * matrixValues[Matrix.MSCALE_Y];

        transX = Math.max(transX, viewWidth - drawableWidth);
        transX = Math.min(transX, 0);

        transY = Math.max(transY, viewHeight - drawableHeight);
        transY = Math.min(transY, 0);

        matrixValues[Matrix.MTRANS_X] = transX;
        matrixValues[Matrix.MTRANS_Y] = transY;
        matrix.setValues(matrixValues);
    }

    private void fixScaleAndTranslation() {
        matrix.getValues(matrixValues);
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];

        scaleX = Math.max(scaleX, 1f);
        scaleY = Math.max(scaleY, 1f);

        matrixValues[Matrix.MSCALE_X] = scaleX;
        matrixValues[Matrix.MSCALE_Y] = scaleY;

        matrix.setValues(matrixValues);
        fixTranslation();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            fixScaleAndTranslation();
            return true;
        }
    }
}