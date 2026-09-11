package tw.nekomimi.nekogram.settings;

import tw.nekomimi.nekogram.MeeroStrings;

import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;

import tw.nekomimi.nekogram.MeeroBubbleStyles;
import tw.nekomimi.nekogram.MeeroGlassTheme;
import tw.nekomimi.nekogram.MeeroTickStyles;
import tw.nekomimi.nekogram.NekoConfig;

/**
 * MeeroX v281 (his triple pick, sealed after the explanation):
 *
 *   a1 - the old hand-painted hero (canvas stripes pretending to be chat,
 *        «قبيحه جدا وليس حقيقيه») is RETIRED. The preview is now a REAL
 *        conversation: actual ChatMessageCell views fed by fabricated
 *        TLRPC messages - the exact pattern Telegram itself ships in
 *        ThemePreviewMessagesCell - over the user's own cached wallpaper.
 *        Bubble outlines come from MessageDrawable, which re-reads
 *        MeeroBubbleStyles on every draw, and the read ticks come from
 *        Theme's own check drawables, so the preview IS the chat.
 *   b2 - rail cards are clean: name (+ description for bubbles) + the
 *        check badge, no tiny canvases; the tapped style is witnessed on
 *        the real strip above.
 *   c1 - on the read-marks tab the strip wears the CANDIDATE pair even
 *        while the master switch is off (preview-only override, cleared
 *        on tab-leave and on dismiss; chats never inherit it).
 *
 * Bonus honesty note: before v281 a tick-style change only reached the
 * chats after a process restart, because Theme's check drawables are
 * built once (null-guarded in createChatResources). The sheet now runs
 * the same official reload (null + createChatResources) that a font
 * change runs, so a pick is live everywhere immediately.
 */
public final class MeeroPickerSheet {

    public static final int TAB_BUBBLES = 0;
    public static final int TAB_TICKS = 1;

    private MeeroPickerSheet() {
    }

    private static int dp(float v) {
        return AndroidUtilities.dp(v);
    }

    private static int blend(int base, int tint, float t) {
        float r = Color.red(base) + (Color.red(tint) - Color.red(base)) * t;
        float g = Color.green(base) + (Color.green(tint) - Color.green(base)) * t;
        float b = Color.blue(base) + (Color.blue(tint) - Color.blue(base)) * t;
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private static GradientDrawable pill(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    /** What the chats would draw right now (master gate honoured). */
    private static int realTickStyle() {
        if (!NekoConfig.meeroTicksSwitch.Bool()) {
            return -1;
        }
        final int s = NekoConfig.meeroTickStyle.Int();
        return (s < 0 || s >= MeeroTickStyles.COUNT) ? 0 : s;
    }

    /** v281: which tick pair Theme's chat drawables were last built with. */
    private static int appliedTicks = Integer.MIN_VALUE;

    /**
     * Points Theme at the wanted tick pair. override >= 0 = preview a
     * candidate regardless of the master switch (c1); -1 = the real state.
     * The Theme override flag is ALWAYS set; the heavier drawable rebuild
     * only runs when the effective pair actually changes.
     */
    private static void syncTickDrawables(Context context, int override) {
        try {
            Theme.meeroTickStylePreviewOverride = override;
            final int desired = override >= 0
                    ? Math.min(override, MeeroTickStyles.COUNT - 1)
                    : realTickStyle();
            if (desired == appliedTicks) {
                return;
            }
            appliedTicks = desired;
            Theme.chat_msgInDrawable = null;
            Theme.createChatResources(context, false);
        } catch (Throwable ignore) {
        }
    }

    public static void open(final Context context, final int firstTab, final Runnable onApply) {
        if (context == null) {
            return;
        }

        final BottomSheet sheet = new BottomSheet(context, false);
        final int[] tab = {firstTab};

        // v128: with the glass skin on, the sheet wears the fixed MeeroX
        // palette like the settings screen (theme-proof, day/night only).
        final boolean glass = MeeroGlassTheme.enabled();
        final int colSheet = glass ? MeeroGlassTheme.sheetBg() : Theme.getColor(Theme.key_dialogBackground);
        final int colInk = glass ? MeeroGlassTheme.ink() : Theme.getColor(Theme.key_dialogTextBlack);
        final int colSub = glass ? MeeroGlassTheme.sub() : Theme.getColor(Theme.key_dialogTextGray3);
        final int colAccent = glass ? MeeroGlassTheme.ACC1 : Theme.getColor(Theme.key_dialogTextBlue);
        final int colSegTrack = glass ? MeeroGlassTheme.segTrack() : Theme.getColor(Theme.key_windowBackgroundGray);

        final LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), 0, dp(14), dp(14));

        // ---- grip ----
        final View grip = new View(context);
        grip.setBackground(pill(blend(colSheet, colSub, 0.35f), 3));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(dp(38), dp(4.5f));
        glp.gravity = Gravity.CENTER_HORIZONTAL;
        glp.topMargin = dp(8);
        glp.bottomMargin = dp(12);
        root.addView(grip, glp);

        // ---- segmented tab pill ----
        final TextView segL = new TextView(context);
        final TextView segR = new TextView(context);
        final LinearLayout seg = new LinearLayout(context);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setBackground(pill(colSegTrack, 13));
        seg.setPadding(dp(3), dp(3), dp(3), dp(3));
        for (TextView tv : new TextView[]{segL, segR}) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(10), dp(7), dp(10), dp(7));
            seg.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        segL.setText(MeeroStrings.s(191));
        segR.setText(MeeroStrings.s(192));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(230), LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.gravity = Gravity.CENTER_HORIZONTAL;
        slp.bottomMargin = dp(10);
        root.addView(seg, slp);

        // ---- title + subtitle ----
        final TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        title.setTextColor(colInk);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final TextView subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        subtitle.setTextColor(colSub);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setText(MeeroStrings.s(188));
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subp.topMargin = dp(2);
        subp.bottomMargin = dp(12);
        root.addView(subtitle, subp);

        // ---- the REAL conversation strip (a1) ----
        final RealChatStrip strip = new RealChatStrip(context);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(196));
        hlp.bottomMargin = dp(12);
        root.addView(strip, hlp);

        // ---- cards rail ----
        final LinearLayout cards = new LinearLayout(context);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        final HorizontalScrollView scroller = new HorizontalScrollView(context);
        scroller.setHorizontalScrollBarEnabled(false);
        // v125: force a plain left-to-right order so the styles read exactly
        // like the agreed mock (newest at the far right, always reachable),
        // and a fade edge advertises that more cards exist.
        scroller.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        cards.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        scroller.setHorizontalFadingEdgeEnabled(true);
        scroller.setFadingEdgeLength(dp(24));
        scroller.addView(cards);
        root.addView(scroller, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // ---- bottom hint ----
        final TextView hint = new TextView(context);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        hint.setTextColor(colSub);
        hint.setGravity(Gravity.CENTER);
        hint.setText(MeeroStrings.s(190));
        LinearLayout.LayoutParams hil = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hil.topMargin = dp(8);
        root.addView(hint, hil);

        final Runnable refreshSeg = () -> {
            final boolean bubbles = tab[0] == TAB_BUBBLES;
            segL.setBackground(pill(bubbles ? colSheet : Color.TRANSPARENT, 10));
            segR.setBackground(pill(!bubbles ? colSheet : Color.TRANSPARENT, 10));
            segL.setTextColor(bubbles ? colInk : colSub);
            segR.setTextColor(!bubbles ? colInk : colSub);
            segL.getPaint().setFakeBoldText(bubbles);
            segR.getPaint().setFakeBoldText(!bubbles);
            title.setText((bubbles ? MeeroStrings.s(35) : MeeroStrings.s(265)));
        };

        // v281: one sync point - the read-marks tab wears the candidate pair
        // (c1), the bubbles tab and the dismiss always fall back to the
        // real master-gated state.
        final Runnable syncStrip = () -> {
            if (tab[0] == TAB_TICKS) {
                final int s = NekoConfig.meeroTickStyle.Int();
                syncTickDrawables(context, (s < 0 || s >= MeeroTickStyles.COUNT) ? 0 : s);
            } else {
                syncTickDrawables(context, -1);
            }
            strip.rebuild();
        };

        // Single-element holder: the card-tap lambda calls back into this
        // runnable, which javac disallows inside its own initializer.
        final Runnable[] rebuildCards = new Runnable[1];
        rebuildCards[0] = () -> {
            cards.removeAllViews();
            final boolean bubbles = tab[0] == TAB_BUBBLES;
            final int count = bubbles ? MeeroBubbleStyles.COUNT : MeeroTickStyles.COUNT;
            final int sel = bubbles ? NekoConfig.meeroBubbleStyle.Int() : NekoConfig.meeroTickStyle.Int();
            for (int i = 0; i < count; i++) {
                cards.addView(makeCard(context, bubbles, i, i == sel, colSheet, colInk, colSub, colAccent, v -> {
                    if (tab[0] == TAB_BUBBLES) {
                        NekoConfig.meeroBubbleStyle.setConfigInt(v);
                    } else {
                        NekoConfig.meeroTickStyle.setConfigInt(v);
                    }
                    if (onApply != null) {
                        onApply.run();
                    }
                    rebuildCards[0].run();
                    syncStrip.run();
                }));
            }
            syncStrip.run();
        };

        segL.setOnClickListener(v -> { tab[0] = TAB_BUBBLES; refreshSeg.run(); rebuildCards[0].run(); });
        segR.setOnClickListener(v -> { tab[0] = TAB_TICKS; refreshSeg.run(); rebuildCards[0].run(); });

        refreshSeg.run();
        rebuildCards[0].run();

        // v281: leaving the sheet ALWAYS restores the real tick pair so a
        // candidate preview can never leak into the chats. (The empty-arg
        // lambda pins BottomSheet's Runnable overload - same pattern
        // VideoAds already uses against this very setter.)
        sheet.setOnDismissListener(() -> syncTickDrawables(context, -1));

        sheet.setCustomView(root);
        sheet.setBackgroundColor(colSheet);
        sheet.fixNavigationBar(colSheet);
        sheet.show();
    }

    private interface CardTap {
        void onTap(int style);
    }

    /**
     * v281 (his pick b2): a clean rail card - name (+ description on the
     * bubbles tab) with the check badge; nothing is painted by hand because
     * the style itself is witnessed on the real conversation strip above.
     */
    private static View makeCard(final Context context, final boolean bubbles, final int style, final boolean selected,
                                 int colSheet, int colInk, int colSub, int colAccent, final CardTap tap) {
        final FrameLayout wrap = new FrameLayout(context);

        final LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dp(8), dp(10), dp(8), dp(10));
        inner.setMinimumHeight(dp(bubbles ? 74 : 50));

        final GradientDrawable bg = pill(colSheet, 16);
        bg.setStroke(dp(selected ? 2f : 1.2f), selected ? colAccent : blend(colSheet, colSub, 0.25f));
        inner.setBackground(bg);

        final TextView name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        name.setTextColor(selected ? colAccent : colInk);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        name.setText(bubbles ? MeeroSettingsActivity.bubbleStyleName(style) : MeeroSettingsActivity.tickStyleName(style));
        inner.addView(name, new LinearLayout.LayoutParams(dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));

        if (bubbles) {
            final TextView desc = new TextView(context);
            desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9.5f);
            desc.setTextColor(colSub);
            desc.setGravity(Gravity.CENTER);
            desc.setMaxLines(2);
            desc.setText(MeeroSettingsActivity.bubbleStyleDesc(style));
            LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(dp(84), LinearLayout.LayoutParams.WRAP_CONTENT);
            dParams.topMargin = dp(3);
            inner.addView(desc, dParams);
        }

        final TextView badge = new TextView(context);
        badge.setText("✓");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        final GradientDrawable bd = new GradientDrawable();
        bd.setShape(GradientDrawable.OVAL);
        bd.setColor(colAccent);
        badge.setBackground(bd);
        badge.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

        FrameLayout.LayoutParams ip2 = new FrameLayout.LayoutParams(dp(100), FrameLayout.LayoutParams.WRAP_CONTENT);
        ip2.setMargins(0, dp(7), 0, 0);
        wrap.addView(inner, ip2);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.TOP | Gravity.START);
        bp.setMarginStart(0);
        wrap.addView(badge, bp);

        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wp.setMargins(dp(3), 0, dp(3), 0);
        wrap.setLayoutParams(wp);

        inner.setOnClickListener(v -> tap.onTap(style));
        return wrap;
    }

    /**
     * v281 (his pick a1): the hero preview - three REAL chat cells fabricated
     * the way Telegram's own ThemePreviewMessagesCell builds its preview
     * (a handmade TLRPC.TL_message wrapped in a MessageObject, fed to a
     * stock ChatMessageCell), stacked over the user's cached wallpaper.
     * Touches are swallowed: this is a showcase, not a chat.
     */
    private static final class RealChatStrip extends FrameLayout {

        private final LinearLayout list;
        private Drawable backgroundDrawable;

        RealChatStrip(Context context) {
            super(context);
            setWillNotDraw(false);
            list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(10), dp(8), dp(10));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            addView(list, lp);
            // the strip is a window into a chat - same rounded mask the old
            // hero panel had.
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(18));
                }
            });
        }

        /** Fabricates one plain text message exactly like the reference. */
        private MessageObject fakeMessage(boolean outgoing, String text, int id) {
            final int date = (int) (System.currentTimeMillis() / 1000) - 60 * 60 + id * 60;
            TLRPC.Message msg = new TLRPC.TL_message();
            msg.message = text;
            msg.date = date;
            msg.dialog_id = 1L;
            // 258 = the reference's 259 minus the unread flag bit: outgoing
            // preview messages are READ, so they wear the double check.
            msg.flags = 258;
            msg.from_id = new TLRPC.TL_peerUser();
            msg.from_id.user_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
            msg.id = id;
            msg.media = new TLRPC.TL_messageMediaEmpty();
            msg.out = outgoing;
            msg.unread = false;
            msg.peer_id = new TLRPC.TL_peerUser();
            msg.peer_id.user_id = outgoing ? 0 : UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
            MessageObject mo = new MessageObject(UserConfig.selectedAccount, msg, true, false);
            mo.eventId = 1;
            mo.resetLayout();
            return mo;
        }

        /** Rebuilds the whole strip pristine - the v260 rebirth trick. */
        void rebuild() {
            list.removeAllViews();
            final MessageObject[] msgs = new MessageObject[]{
                    fakeMessage(false, MeeroStrings.s(111), 1),
                    fakeMessage(true, MeeroStrings.s(112), 2),
                    fakeMessage(false, MeeroStrings.s(113), 3)
            };
            for (final MessageObject mo : msgs) {
                final ChatMessageCell cell = new ChatMessageCell(getContext(), UserConfig.selectedAccount,
                        false, null, null) {
                    @Override
                    public boolean onTouchEvent(MotionEvent event) {
                        return false;
                    }
                };
                cell.setFullyDraw(true);
                cell.setMessageObject(mo, null, false, false, false);
                list.addView(cell, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            // a soft fade settles the rebuild instead of a hard flash.
            setAlpha(0f);
            animate().alpha(1f).setDuration(220).start();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Wallpaper: the same proven path ThemePreviewMessagesCell draws.
            Drawable d = Theme.getCachedWallpaperNonBlocking();
            if (Theme.wallpaperLoadTask != null) {
                invalidate();
            }
            if (d != backgroundDrawable && d != null) {
                backgroundDrawable = d;
            }
            if (backgroundDrawable != null) {
                if (backgroundDrawable instanceof ColorDrawable
                        || backgroundDrawable instanceof GradientDrawable
                        || backgroundDrawable instanceof org.telegram.ui.Components.MotionBackgroundDrawable) {
                    backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    backgroundDrawable.draw(canvas);
                } else if (backgroundDrawable instanceof BitmapDrawable) {
                    final BitmapDrawable bitmapDrawable = (BitmapDrawable) backgroundDrawable;
                    bitmapDrawable.setFilterBitmap(true);
                    if (bitmapDrawable.getTileModeX() == Shader.TileMode.REPEAT) {
                        canvas.save();
                        float scale = 2.0f / AndroidUtilities.density;
                        canvas.scale(scale, scale);
                        bitmapDrawable.setBounds(0, 0, (int) Math.ceil(getMeasuredWidth() / scale), (int) Math.ceil(getMeasuredHeight() / scale));
                    } else {
                        int viewHeight = getMeasuredHeight();
                        float scaleX = (float) getMeasuredWidth() / (float) bitmapDrawable.getIntrinsicWidth();
                        float scaleY = (float) viewHeight / (float) bitmapDrawable.getIntrinsicHeight();
                        float scale = Math.max(scaleX, scaleY);
                        int width = (int) Math.ceil(bitmapDrawable.getIntrinsicWidth() * scale);
                        int height = (int) Math.ceil(bitmapDrawable.getIntrinsicHeight() * scale);
                        int x = (getMeasuredWidth() - width) / 2;
                        int y = (viewHeight - height) / 2;
                        canvas.save();
                        canvas.clipRect(0, 0, width, getMeasuredHeight());
                        bitmapDrawable.setBounds(x, y, x + width, y + height);
                    }
                    backgroundDrawable.draw(canvas);
                    canvas.restore();
                } else {
                    backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    backgroundDrawable.draw(canvas);
                }
            } else {
                canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundGray));
            }
        }
    }
}
