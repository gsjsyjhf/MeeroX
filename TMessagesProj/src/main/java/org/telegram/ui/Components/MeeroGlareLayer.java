package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * MeeroX v254 — Cherrygram "Glare effects" (his sealed order).
 *
 * A soft diagonal shine that sweeps periodically across a surface: the
 * centered chat-header pill (ChatAvatarContainer) and chat bubbles
 * (ChatMessageCell). Pure paint-time overlay, driven by the caller's frame
 * invalidation loop — no animators, no allocation of views.
 */
public class MeeroGlareLayer {

    private static final long PERIOD = 2400L; // one sweep duration, ms
    private static final long IDLE = 1800L;   // rest between sweeps, ms
    private static final int SHINE = 0x2EFFFFFF;

    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF band = new RectF();
    private static final RectF clip = new RectF();

    /** Draws one moving shine band inside [l,t,r,b] for the given clock time. */
    public static void draw(Canvas canvas, float l, float t, float r, float b, float radius, long now) {
        final float w = r - l, h = b - t;
        if (w <= 8 || h <= 8) {
            return;
        }
        final float phase = (float) ((now % (PERIOD + IDLE)) / (double) PERIOD);
        if (phase > 1f) {
            return; // resting between sweeps
        }
        final float bandW = w * 0.65f;
        final float cx = l - bandW / 2f + phase * (w + bandW);
        clip.set(l, t, r, b);
        band.set(cx - bandW / 2f, t - h, cx + bandW / 2f, b + h);
        canvas.save();
        canvas.clipRect(clip);
        canvas.translate((l + r) / 2f, (t + b) / 2f);
        canvas.rotate(-16);
        canvas.translate(-(l + r) / 2f, -(t + b) / 2f);
        paint.setShader(new LinearGradient(cx - bandW / 2f, 0, cx + bandW / 2f, 0,
                new int[]{0x00000000, SHINE, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(band, paint);
        paint.setShader(null);
        canvas.restore();
    }
}
