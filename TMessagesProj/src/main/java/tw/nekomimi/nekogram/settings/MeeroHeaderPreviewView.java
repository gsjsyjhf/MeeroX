package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

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
 * MeeroX v256 (his sealed order): LIVE chat-top-strip preview for the new
 * collapsible "شريط الدردشة العلوي" section in Meero settings. It hosts the
 * app's REAL header widgets (ActionBar + ChatAvatarContainer over the user's
 * actual chat wallpaper, with his own account name and photo), so every
 * switch in the section repaints the genuine thing with zero fake drawing:
 *   - centered glass pill            <- meeroCherryTitle    (gated by stock)
 *   - adaptive pill width            <- meeroCherryAdaptive (gated by title)
 *   - animated glare sweep           <- meeroGlare (drawn by the real widget)
 *   - unread chip on the back button <- unreadBadgeOnBackButton (demo count 10)
 *   - "رجوع للأصلي"                  <- meeroHeaderStock (forces stock layout)
 * A light self-refresh loop watches the configs so toggling a row updates the
 * preview even though the list never rebuilds this cell.
 */
public class MeeroHeaderPreviewView extends FrameLayout {

    private final ActionBar actionBar;
    private final ChatAvatarContainer avatarContainer;
    private Drawable backgroundDrawable;

    private boolean lastCentered;
    private boolean lastAdaptive;

    public MeeroHeaderPreviewView(Context context, BaseFragment fragment, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        actionBar = new ActionBar(context);
        actionBar.setOccupyStatusBar(false);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault, resourcesProvider));
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector, resourcesProvider), false);
        actionBar.setCastShadows(false);

        actionBar.setBackButtonDrawable(new BackDrawable(false));

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem menuItem = menu.addItem(0, R.drawable.ic_ab_other);
        menuItem.setContentDescription(getString(R.string.AccDescrMoreOptions));

        // Same glass plumbing ChatActivity uses (color source variant - the
        // wallpaper behind is painted by this view itself in onDraw).
        BlurredBackgroundSourceColor sourceColor = new BlurredBackgroundSourceColor();
        sourceColor.setColor(fragment.getThemedColor(Theme.key_windowBackgroundWhite));
        BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(sourceColor);
        actionBar.setupGlass(factory, BlurredBackgroundProviderImpl.topPanelChatActivity(resourcesProvider));

        addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.CENTER_VERTICAL, 6, 4, 6, 4));

        avatarContainer = new ChatAvatarContainer(context, fragment, true, resourcesProvider) {
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
            avatarContainer.setUserAvatar(user);
        }
        avatarContainer.setGlassMode();
        avatarContainer.allowShorterStatus = true;
        avatarContainer.premiumIconHiddable = false;
        avatarContainer.allowDrawStories = false;
        avatarContainer.setClipChildren(false);

        avatarContainer.setTitle(meeroPreviewTitle(user));
        avatarContainer.setSubtitle(getString(R.string.Online));

        actionBar.addView(avatarContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 54, 0, 54, 0));
        // MeeroX v254 wiring: the centered pill lives in the dedicated slot and
        // the adaptive-width reader hangs off it - mirrors ChatActivity exactly.
        actionBar.setChatAvatarContainer2(avatarContainer);

        lastCentered = meeroEffectiveCentered();
        lastAdaptive = meeroEffectiveAdaptive();
        actionBar.setForceAdaptiveWidth(lastAdaptive);
        meeroRefreshBadge();
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

    private String meeroPreviewTitle(TLRPC.User user) {
        if (user != null && (!TextUtils.isEmpty(user.first_name) || !TextUtils.isEmpty(user.last_name))) {
            return ContactsController.formatName(user.first_name, user.last_name);
        }
        return "MeeroX"; // no name on account yet - brand placeholder
    }

    private void meeroRefreshBadge() {
        if (actionBar.backButtonImageView == null) {
            return;
        }
        if (NekoConfig.unreadBadgeOnBackButton.Bool()) {
            actionBar.unreadBadgeSetCount(10); // demo count; real chat feeds the true number
        } else {
            actionBar.backButtonImageView.setUnread(0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The user's real chat wallpaper behind the header, cover-scaled.
        Drawable drawable = Theme.getCachedWallpaperNonBlocking();
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

    // ---- self-refresh loop: configs -> live repaint ----

    private final Runnable meeroRefresher = new Runnable() {
        @Override
        public void run() {
            boolean centered = meeroEffectiveCentered();
            boolean adaptive = meeroEffectiveAdaptive();
            if (centered != lastCentered || adaptive != lastAdaptive) {
                lastCentered = centered;
                lastAdaptive = adaptive;
                actionBar.setForceAdaptiveWidth(adaptive);
                actionBar.requestLayout();
                avatarContainer.requestLayout();
                invalidate();
            }
            meeroRefreshBadge();
            // glare sweep frames (only while the pill can actually shine)
            try {
                if (centered && NekoConfig.meeroGlare.Bool()) {
                    avatarContainer.invalidate();
                }
            } catch (Throwable ignore) {
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
}
