package org.telegram.ui.Components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

/**
 * MeeroX v250-v252 — developer-profile header wallpaper (his sealed orders, dev = @i55544).
 *
 * Painted by ProfileActivity.TopView.onDraw INSIDE the stock header block
 * only (v252: he wants the pattern strictly top-only like ellipi; below the
 * header the profile keeps the stock look). Only active when the opened
 * profile belongs to the developer account. Two modes (NekoConfig
 * "meeroDevProfileBg"): 0 = tiled name pattern over the approved grey-blue
 * gradient (the name is read live from the profile, so renaming himself
 * re-renders the pattern), 1 = his current profile photo centre-cropped and
 * blurred as a dimmed backdrop (follows photo changes too). Mode 2 = off,
 * handled by ProfileActivity not drawing this drawable at all.
 *
 * All drawing is local to the viewer's MeeroX build; official clients see
 * the untouched stock profile.
 */
public class MeeroDevProfileBgDrawable extends Drawable {

    /** Tiny supplier (avoids java.util.function callback classes). */
    public interface MeeroBitmapSource {
        Bitmap get();
    }

    private static final int[] GRAD = new int[]{0xFF4A5A70, 0xFF39445A, 0xFF2C3547};
    private static final float DIM_PATTERN = 72f / 255f;  // ~28% (his preview default)
    private static final float DIM_PHOTO = 140f / 255f;   // ~55% like the sealed mock

    private final Paint shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final MeeroBitmapSource bitmapSource;
    private final Runnable retryInvalidate = this::invalidateSelf;

    private int mode = 0;
    private String name = "";
    private LinearGradient shader;
    private Bitmap blurredFrom, blurred;
    private boolean waitingForBitmap;

    public MeeroDevProfileBgDrawable(MeeroBitmapSource bitmapSource) {
        this.bitmapSource = bitmapSource;
        textPaint.setColor(0xFF0D1117);
        textPaint.setAlpha((int) (DIM_PATTERN * 255));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(AndroidUtilities.dp(19));
    }

    public void setMode(int mode) {
        if (this.mode != mode) {
            this.mode = mode;
            invalidateSelf();
        }
    }

    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        if (!name.equals(this.name)) {
            this.name = name;
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        shader = null;
        invalidateSelf();
    }

    private Bitmap makeBlurredCover(Bitmap src, int w, int h) {
        if (src == null || src.isRecycled() || w <= 0 || h <= 0) {
            return null;
        }
        int s = Math.min(src.getWidth(), src.getHeight());
        if (s <= 0) {
            return null;
        }
        int tw = Math.min(480, Math.max(160, w / 2));
        int th = Math.max(160, (int) ((long) tw * h / Math.max(1, w)));
        Bitmap crop = Bitmap.createBitmap(src, (src.getWidth() - s) / 2, (src.getHeight() - s) / 2, s, s);
        Bitmap scaled = Bitmap.createScaledBitmap(crop, tw, th, true);
        if (scaled != crop) {
            crop.recycle();
        }
        try {
            Utilities.stackBlurBitmap(scaled, 24);
        } catch (Throwable ignore) {}
        return scaled;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) {
            return;
        }
        if (mode == 1) {
            Bitmap src = null;
            if (bitmapSource != null) {
                try {
                    src = bitmapSource.get();
                } catch (Throwable ignore) {}
            }
            if (src != null && !src.isRecycled()) {
                if (blurredFrom != src || blurred == null || blurred.isRecycled()) {
                    if (blurred != null && !blurred.isRecycled() && blurred != src) {
                        blurred.recycle();
                    }
                    blurred = makeBlurredCover(src, b.width(), b.height());
                    blurredFrom = src;
                }
                waitingForBitmap = false;
                if (blurred != null && !blurred.isRecycled()) {
                    canvas.drawBitmap(blurred, null, b, null);
                    canvas.drawColor(Color.argb((int) (DIM_PHOTO * 255), 0, 0, 0));
                    return;
                }
            } else {
                // photo still loading - retry soon, fall through to gradient meanwhile
                if (!waitingForBitmap) {
                    waitingForBitmap = true;
                    scheduleSelf(retryInvalidate, android.os.SystemClock.uptimeMillis() + 350);
                }
            }
        }
        if (shader == null) {
            shader = new LinearGradient(0, b.top, b.width() * 0.65f, b.bottom, GRAD, null, Shader.TileMode.CLAMP);
            shaderPaint.setShader(shader);
        }
        canvas.drawPaint(shaderPaint);
        if (mode == 0 && !name.isEmpty()) {
            final float textW = Math.max(AndroidUtilities.dp(40), textPaint.measureText(name));
            final float stepX = textW + AndroidUtilities.dp(52);
            final float stepY = AndroidUtilities.dp(64);
            canvas.save();
            canvas.rotate(-18, b.exactCenterX(), b.exactCenterY());
            float y0 = -b.height() * 0.55f;
            float yEnd = b.height() * 1.55f;
            int row = 0;
            for (float y = y0; y < yEnd; y += stepY, row++) {
                float x0 = -b.width() * 0.55f - ((row % 2) * stepX * 0.5f);
                for (float x = x0; x < b.width() * 1.55f; x += stepX) {
                    canvas.drawText(name, x, y, textPaint);
                }
            }
            canvas.restore();
        }
        canvas.drawColor(Color.argb((int) (DIM_PATTERN * 255), 0, 0, 0));
    }

    @Override
    public void setAlpha(int alpha) {
        // drawn fully self-composited; kept for Drawable contract
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        shaderPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
