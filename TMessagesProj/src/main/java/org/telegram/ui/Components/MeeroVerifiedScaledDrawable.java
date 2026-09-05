package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * MeeroX v248 — verified badge enlargement (his final slider pick = 118% (1.18x)).
 *
 * Wraps the stock verified area/check drawables: reports a scaled intrinsic
 * size (so every cell that reserves space for the badge grows with it
 * automatically) and draws the inner drawable scaled about the bounds centre.
 * Everything else (alpha, color filters, state, constant-state cloning for
 * ProfileActivity's crossfade drawables) is delegated to the inner drawable.
 *
 * Applied in Theme to dialogs_verifiedDrawable / dialogs_verifiedCheckDrawable
 * / profile_verifiedDrawable / profile_verifiedCheckDrawable, and locally in
 * ChatAvatarContainer for the chat-title badge.
 */
public class MeeroVerifiedScaledDrawable extends Drawable {

    public static float scale = 1.18f;

    private final Drawable inner;

    public MeeroVerifiedScaledDrawable(Drawable inner) {
        this.inner = inner;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        canvas.save();
        canvas.scale(scale, scale, b.centerX(), b.centerY());
        inner.setBounds(b);
        inner.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getIntrinsicWidth() {
        return Math.round(inner.getIntrinsicWidth() * scale);
    }

    @Override
    public int getIntrinsicHeight() {
        return Math.round(inner.getIntrinsicHeight() * scale);
    }

    @Override
    public void setAlpha(int alpha) {
        inner.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        inner.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(int color, PorterDuff.Mode mode) {
        inner.setColorFilter(color, mode);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return inner.getColorFilter();
    }

    @Override
    public int getOpacity() {
        return inner.getOpacity();
    }

    @Override
    public boolean isStateful() {
        return inner.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return inner.setState(state);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        inner.setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        inner.setBounds(bounds);
    }

    @Override
    public Drawable mutate() {
        inner.mutate();
        return this;
    }

    /** Clones stay wrapped+scaled (ProfileActivity newDrawable().mutate()). */
    @Override
    public ConstantState getConstantState() {
        final ConstantState cs = inner.getConstantState();
        if (cs == null) {
            return null;
        }
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                return new MeeroVerifiedScaledDrawable(cs.newDrawable());
            }

            @Override
            public Drawable newDrawable(android.content.res.Resources res) {
                return new MeeroVerifiedScaledDrawable(cs.newDrawable(res));
            }

            @Override
            public Drawable newDrawable(android.content.res.Resources res, android.content.res.Resources.Theme theme) {
                return new MeeroVerifiedScaledDrawable(cs.newDrawable(res, theme));
            }

            @Override
            public int getChangingConfigurations() {
                return cs.getChangingConfigurations();
            }
        };
    }
}
