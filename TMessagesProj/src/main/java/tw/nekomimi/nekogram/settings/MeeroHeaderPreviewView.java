package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;

import tw.nekomimi.nekogram.NekoConfig;

/**
 * MeeroX v260 (his round 4 - «شخص السبب مية بالمية»):
 *
 *   1. "الصورة ضبطت" - the v259 avatar-over-⋮ trick (no end margin while
 *      centered, avatar pins the far corner like the reference) STAYS.
 *   2. "الكبسولة خارجة عن الاسم" - v259 ALSO hand-centered the title via
 *      custom container math, while the glass pill is positioned by the
 *      ActionBar's OWN adaptive machinery. Two coordinate systems, one bar:
 *      the text slid off its capsule. Reverted to the exact v258 layout
 *      math (proven on HIS device) - container/pill couple again.
 *   3. Bar stays 70dp edge-to-edge (reference parity from v259).
 *
 * Kept from v257/v258: pristine meeroBuild() rebirth on every config flip,
 * menu.setCenteredTitle wiring, real name/photo (showSelf=true),
 * guaranteed wallpaper, white-chip back capsule (preview-only; the real
 * chat keeps his red chip), adaptive glass pill, live glare, plain-stock
 * mode, no double-chevron ghost.
 */
public class MeeroHeaderPreviewView extends FrameLayout {

    private final BaseFragment fragment;
    private final Theme.ResourcesProvider resourcesProvider;
    private ActionBar actionBar;
    private ChatAvatarContainer avatarContainer;
    private MeeroBackCapsule backCapsule;
    private Drawable backgroundDrawable;
    private boolean wallpaperKickDone;

    private boolean lastCentered;
    private boolean lastAdaptive;

    public MeeroHeaderPreviewView(Context context, BaseFragment fragment, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.fragment = fragment;
        this.resourcesProvider = resourcesProvider;
        meeroBuild();
    }

    /** (Re)builds the whole header pristine, at the configs' final state —
     *  the same trick the reference's settings list achieves by rebuilding
     *  the entire row on every toggle. */
    private void meeroBuild() {
        removeAllViews();
        lastCentered = meeroEffectiveCentered();
        lastAdaptive = meeroEffectiveAdaptive();

        actionBar = new ActionBar(getContext());
        actionBar.setOccupyStatusBar(false);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault, resourcesProvider));
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector, resourcesProvider), false);
        actionBar.setCastShadows(false);

        actionBar.setBackButtonDrawable(new BackDrawable(false));

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem menuItem = menu.addItem(0, R.drawable.ic_ab_other);
        menuItem.setContentDescription(getString(R.string.AccDescrMoreOptions));
        // MeeroX v265: mirror the real-chat fix - in the centered look the
        // avatar replaces the ⋮ slot entirely (its glass circle must not peek
        // from behind the avatar). Alpha 0 keeps the layout untouched.
        menuItem.setAlpha(lastCentered ? 0f : 1f);
        // MeeroX v258 fix: THE missing wire. ChatActivity:4692 does
        // menu.setCenteredTitle(isTitleCentered()) - without it the bar laid
        // out half-stock/half-pill (ghost title + empty pill).
        menu.setCenteredTitle(lastCentered);

        BlurredBackgroundSourceColor sourceColor = new BlurredBackgroundSourceColor();
        sourceColor.setColor(fragment.getThemedColor(Theme.key_windowBackgroundWhite));
        BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(sourceColor);
        actionBar.setupGlass(factory, BlurredBackgroundProviderImpl.topPanelChatActivity(resourcesProvider));

        // v259: reference-exact bar frame - 70dp tall, edge to edge.
        addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 70, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        avatarContainer = new ChatAvatarContainer(getContext(), fragment, false, resourcesProvider) {
            @Override
            public boolean isCentered() {
                return meeroEffectiveCentered();
            }

            @Override
            protected boolean useAnimatedSubtitle() {
                return true;
            }
        };
        avatarContainer.setActionBar(actionBar);
        avatarContainer.setOccupyStatusBar(false);

        final TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        if (user != null) {
            // showSelf=true: REAL photo/name path, no saved-messages render
            avatarContainer.setUserAvatar(user, true);
        }
        avatarContainer.setGlassMode();
        avatarContainer.allowShorterStatus = true;
        avatarContainer.premiumIconHiddable = false;
        avatarContainer.allowDrawStories = false;
        avatarContainer.setClipChildren(false);

        avatarContainer.setTitle(meeroPreviewTitle(user));
        avatarContainer.setSubtitle(getString(R.string.Online));

        // avatar pinned on the ⋮ corner (his approved «الصورة ضبطت»): no end
        // margin while centered, the avatar covers the menu spot like the
        // reference. PLUS (v261) the pixel-exact title centering re-enabled
        // - dp(9) this time, the v259 mishap was a dp/px unit slip.
        avatarContainer.setMeeroPreviewTitleCenter(true);
        actionBar.addView(avatarContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 54, 0, lastCentered ? 0 : 54, 0));
        // MeeroX v264: preview mirrors the real chat fix - avatar paints above
        // the ⋮ glyph, never under it («الصورة فوق الـ3 نقاط»).
        avatarContainer.bringToFront();
        actionBar.setChatAvatarContainer2(avatarContainer);

        backCapsule = new MeeroBackCapsule(getContext(),
                Theme.getColor(Theme.key_actionBarDefault, resourcesProvider),
                Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider));
        addView(backCapsule, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        actionBar.setForceAdaptiveWidth(lastAdaptive);
        // v261: (re)compute the adaptive pill target against THIS title -
        // real ChatActivity does this on every layout pass, the preview never
        // did, so a stale/wrong-width capsule could keep hugging old text.
        actionBar.checkAvatarContainerWidth(false);
        meeroRefreshCapsule();
        meeroRefreshBackVisuals();
    }

    private static boolean meeroEffectiveCentered() {
        try {
            return NekoConfig.meeroCherryTitle.Bool() && !NekoConfig.meeroHeaderStock.Bool();
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static boolean meeroEffectiveAdaptive() {
        try {
            return meeroEffectiveCentered() && NekoConfig.meeroCherryAdaptive.Bool();
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static boolean meeroBadgeOn() {
        try {
            return NekoConfig.unreadBadgeOnBackButton.Bool();
        } catch (Throwable ignore) {
            return false;
        }
    }

    private String meeroPreviewTitle(TLRPC.User user) {
        if (user != null && (!TextUtils.isEmpty(user.first_name) || !TextUtils.isEmpty(user.last_name))) {
            return ContactsController.formatName(user.first_name, user.last_name);
        }
        return "MeeroX"; // no name on account yet - brand placeholder
    }

    /** Reference parity: capsule visible only in pill mode; chip per switch. */
    private void meeroRefreshCapsule() {
        backCapsule.setState(lastCentered, meeroBadgeOn());
        // MeeroX v275 (his report «الكبسولة تندمج مع كبسولة الرجوع» with the
        // adaptive switch OFF): our hand-made back capsule sits at
        // [8dp .. 8dp + 63/38dp] while the real button stays INVISIBLE, so
        // the bar's stock pill math started the name capsule at x=0 and the
        // two merged. Report the capsule's right edge - the name capsule
        // now starts exactly after it (separate pills, natural full length,
        // reference OFF behaviour). Lives here, not in meeroBuild, because
        // the badge switch flips the capsule width without a rebuild.
        actionBar.setMeeroPreviewBackZoneEnd(
                lastCentered ? dp(8) + dp(meeroBadgeOn() ? 63 : 38) : -1);
    }

    /** While the capsule is up, the stock back button must not draw under it
     *  (v257's double-chevron ghost). INVISIBLE keeps its layout metrics. */
    private void meeroRefreshBackVisuals() {
        if (actionBar.backButtonImageView != null) {
            actionBar.backButtonImageView.setVisibility(lastCentered ? INVISIBLE : VISIBLE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The user's real chat wallpaper behind the header, cover-scaled.
        Drawable drawable = Theme.getCachedWallpaperNonBlocking();
        if (drawable == null && !wallpaperKickDone) {
            // One guaranteed load off the UI thread, then repaint.
            wallpaperKickDone = true;
            new Thread(() -> {
                Theme.getCachedWallpaper();
                postInvalidate();
            }, "meero-hdr-wallpaper").start();
        }
        if (Theme.wallpaperLoadTask != null) {
            invalidate();
        }
        if (drawable != backgroundDrawable) {
            backgroundDrawable = drawable;
        }
        if (drawable != null) {
            drawable.setAlpha(255);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmapDrawable.setFilterBitmap(true);
                float scale = Math.max((float) getMeasuredWidth() / drawable.getIntrinsicWidth(),
                        (float) getMeasuredHeight() / drawable.getIntrinsicHeight());
                int w = (int) Math.ceil(drawable.getIntrinsicWidth() * scale);
                int h = (int) Math.ceil(drawable.getIntrinsicHeight() * scale);
                int x = (getMeasuredWidth() - w) / 2;
                int y = (getMeasuredHeight() - h) / 2;
                drawable.setBounds(x, y, x + w, y + h);
            } else {
                drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
            drawable.draw(canvas);
        } else {
            canvas.drawColor(Theme.getColor(Theme.key_chat_wallpaper));
        }
        super.onDraw(canvas);
    }

    // ---- self-refresh loop: configs -> pristine rebuild on flip ----

    private final Runnable meeroRefresher = new Runnable() {
        @Override
        public void run() {
            boolean centered = meeroEffectiveCentered();
            boolean adaptive = meeroEffectiveAdaptive();
            if (centered != lastCentered || adaptive != lastAdaptive) {
                meeroBuild(); // pristine rebirth, reference-rebuild style
            } else {
                meeroRefreshCapsule();
                try {
                    if (centered && NekoConfig.meeroGlare.Bool()) {
                        avatarContainer.invalidate();
                    }
                } catch (Throwable ignore) {
                }
            }
            if (isAttachedToWindow()) {
                postDelayed(this, 260);
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(meeroRefresher);
        postDelayed(meeroRefresher, 260);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(meeroRefresher);
        super.onDetachedFromWindow();
    }

    /**
     * The reference's back capsule - a glass pill around the chevron carrying
     * the WHITE count chip inside. Pure cosmetics for this preview (our real
     * chat header keeps the owner's red chip).
     */
    private static final class MeeroBackCapsule extends View {

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint chevronPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint chipTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path chevron = new Path();
        private final Rect textBounds = new Rect();

        private boolean show;
        private boolean chip;

        MeeroBackCapsule(Context context, int actionBarColor, int itemsColor) {
            super(context);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(ColorUtils.blendARGB(actionBarColor, Color.WHITE, 0.10f));
            bgPaint.setAlpha(242);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(Math.max(1f, dp(0.66f)));
            strokePaint.setColor(ColorUtils.blendARGB(actionBarColor, Color.WHITE, 0.28f));
            strokePaint.setAlpha(110);
            chevronPaint.setStyle(Paint.Style.STROKE);
            chevronPaint.setStrokeWidth(dp(1.9f));
            chevronPaint.setStrokeCap(Paint.Cap.ROUND);
            chevronPaint.setStrokeJoin(Paint.Join.ROUND);
            chevronPaint.setColor(itemsColor);
            chipPaint.setStyle(Paint.Style.FILL);
            chipPaint.setColor(Color.WHITE);
            chipTextPaint.setColor(ColorUtils.blendARGB(actionBarColor, Color.BLACK, 0.55f));
            chipTextPaint.setTextSize(dp(12));
            chipTextPaint.setFakeBoldText(true);
            updateVisibilityState();
        }

        void setState(boolean capsuleVisible, boolean chipVisible) {
            if (show != capsuleVisible || chip != chipVisible) {
                show = capsuleVisible;
                chip = chipVisible;
                updateVisibilityState();
                requestLayout();
                invalidate();
            }
        }

        private void updateVisibilityState() {
            setVisibility(show ? VISIBLE : GONE);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(chip ? dp(63) : dp(38), dp(34));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float w = getWidth(), h = getHeight();
            rect.set(0.5f, 0.5f, w - 0.5f, h - 0.5f);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, bgPaint);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, strokePaint);

            // chevron zone (left when chip shown, centered when alone)
            float zoneW = chip ? w - dp(33) : w;
            float cx = zoneW / 2f - dp(1), cy = h / 2f;
            float dx = dp(4.2f), dy = dp(6.6f);
            chevron.reset();
            chevron.moveTo(cx + dx, cy - dy);
            chevron.lineTo(cx - dx, cy);
            chevron.lineTo(cx + dx, cy + dy);
            canvas.drawPath(chevron, chevronPaint);

            if (chip) {
                // white count chip "10" inside the capsule, reference style
                String s = "10";
                chipTextPaint.getTextBounds(s, 0, s.length(), textBounds);
                float tw = chipTextPaint.measureText(s);
                float cw = tw + dp(14), chh = dp(21);
                float cl = w - dp(7.5f) - cw, ct = (h - chh) / 2f;
                rect.set(cl, ct, cl + cw, ct + chh);
                canvas.drawRoundRect(rect, chh / 2f, chh / 2f, chipPaint);
                float tx = cl + (cw - tw) / 2f;
                float ty = ct + chh / 2f - (chipTextPaint.descent() + chipTextPaint.ascent()) / 2f;
                canvas.drawText(s, tx, ty, chipTextPaint);
            }
        }
    }
}
