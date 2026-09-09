package tw.nekomimi.nekogram;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.CreationTextCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckCell2;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.Switch;

import java.util.ArrayList;

import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.AccountCell;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * MeeroX v129: shared "glass skin" toolkit. MeeroSettingsActivity (newer
 * base) and the fourteen legacy-base Meero screens both render their rows
 * through these helpers, so the look stays identical everywhere and the
 * logic lives in exactly one place.
 *
 * Every entry point takes the live glass state and either applies the fixed
 * palette look or restores the exact stock look, so both master toggles
 * flip with a plain notifyDataSetChanged().
 */
public final class MeeroGlassSupport {

    private MeeroGlassSupport() {
    }

    // Card edge flags shared by both generations of screens.
    public static final int CARD_NONE = 0;
    public static final int CARD_SINGLE = 1;
    public static final int CARD_TOP = 2;
    public static final int CARD_MID = 3;
    public static final int CARD_BOTTOM = 4;

    /* v187 (batch 3A): every design number below arrives from the sealed
     * native table; legacy literals remain as the exact fallback. */
    private static float[] sUi;
    private static float uic(int i, float fb) {
        float[] u = sUi;
        if (u == null) {
            float[] n = MeeroCore.glassCore() ? MeeroCore.nGlassUiConsts() : null;
            u = (n != null && n.length == 32) ? n : new float[0];
            sUi = u;
        }
        return u.length == 32 ? u[i] : fb;
    }

    /** Tag for the gradient rule view injected under section headers. */
    public static final String GLASS_RULE = "meeroGlassHeaderRule";

    // ------------------------------------------------------------------
    // Sections (the v128 scroll-wash fix), now for any screen.
    // ------------------------------------------------------------------

    /**
     * While glass is on we re-install the SAME section geometry the stock
     * base uses (12dp/16dp/topPadding) but with a painter that paints
     * NOTHING - our cards are the only cards in town - and the row selector
     * is pinned to the fixed press tint. OFF restores the exact stock call.
     */
    public static void applySectionsSkin(RecyclerListView listView, boolean glass,
                                         Utilities.CallbackReturn<View, Boolean> keepRow) {
        if (listView == null) {
            return;
        }
        if (glass) {
            listView.setSections(keepRow,
                    AndroidUtilities.dp(uic(0, 12)), AndroidUtilities.dp(uic(1, 16)),
                    (canvas, rect, rx, ry, alpha) -> { /* glass: no themed section paint */ },
                    true);
            listView.setSelectorDrawableColor(MeeroGlassTheme.press());
        } else {
            listView.setSections(true);
        }
    }

    /** Rows that keep a section gap in legacy-base screens = the card rows. */
    public static boolean legacySectionsKeep(View view) {
        return view instanceof TextCheckCell
                || view instanceof TextCheckCell2
                || view instanceof TextSettingsCell
                || view instanceof TextDetailSettingsCell
                || view instanceof TextCell
                || view instanceof TextRadioCell
                || view instanceof TextCheckbox2Cell
                || view instanceof CheckBoxCell
                || view instanceof NotificationsCheckCell
                || view instanceof CreationTextCell
                || view instanceof AccountCell;
    }

    // ------------------------------------------------------------------
    // Row skinning
    // ------------------------------------------------------------------

    /** Card rows breathe in from the sides; structural rows span full width. */
    public static void setRowMargins(View v, boolean inCard) {
        if (!(v.getLayoutParams() instanceof RecyclerView.LayoutParams)) {
            return;
        }
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
        int side = inCard ? AndroidUtilities.dp(uic(2, 12)) : 0;
        if (lp.leftMargin != side || lp.rightMargin != side || lp.topMargin != 0 || lp.bottomMargin != 0) {
            lp.setMargins(side, 0, side, 0);
            v.setLayoutParams(lp);
        }
    }

    /** Header rows in the glass look: small accent title + gradient rule. */
    public static void styleHeaderCell(View v, boolean glass) {
        if (!(v instanceof HeaderCell)) {
            return;
        }
        HeaderCell h = (HeaderCell) v;
        if (glass) {
            v.setBackgroundColor(Color.TRANSPARENT);
            h.setTextColor(MeeroGlassTheme.headerInk());
            TextView tv = h.getTextView();
            if (tv != null) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, uic(7, 13));
                tv.setLetterSpacing(uic(9, 0.04f));
            }
            View rule = h.findViewWithTag(GLASS_RULE);
            if (rule == null && h.getContext() != null) {
                rule = new View(h.getContext());
                rule.setTag(GLASS_RULE);
                rule.setBackground(MeeroGlassTheme.headerRule());
                h.addView(rule, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 2, 0, 4, 0, 0));
            }
            rule.setVisibility(View.VISIBLE);
        } else {
            h.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
            TextView tv = h.getTextView();
            if (tv != null) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, uic(8, 15));
                tv.setLetterSpacing(0f);
            }
            View rule = h.findViewWithTag(GLASS_RULE);
            if (rule != null) {
                rule.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Selecting rows wear their current value as a rose chip. The value
     * label in TextSettingsCell is an AnimatedTextView, so it is found by
     * type rather than by the TextView walk.
     */
    public static void styleValueChip(View v, boolean glass) {
        if (!(v instanceof TextSettingsCell)) {
            return;
        }
        AnimatedTextView value = findAnimatedText(v);
        if (value == null) {
            return;
        }
        if (glass) {
            value.setTextColor(MeeroGlassTheme.ACC1);
            value.setTextSize(AndroidUtilities.dp(uic(5, 13)));
            value.setBackground(MeeroGlassTheme.chipBg());
            value.setPadding(AndroidUtilities.dp(uic(3, 9)), AndroidUtilities.dp(uic(4, 1)),
                    AndroidUtilities.dp(uic(3, 9)), AndroidUtilities.dp(uic(4, 1)));
        } else {
            value.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            value.setTextSize(AndroidUtilities.dp(uic(6, 16)));
            value.setBackground(null);
            value.setPadding(0, 0, 0, 0);
        }
    }

    public static AnimatedTextView findAnimatedText(View v) {
        if (v instanceof AnimatedTextView) {
            return (AnimatedTextView) v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                AnimatedTextView found = findAnimatedText(g.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** First TextView in a cell is its title; the rest are subtitles/values. */
    public static void tintCellText(View v, int titleColor, int subColor) {
        ArrayList<TextView> texts = new ArrayList<>(4);
        collectTextViews(v, texts);
        for (int i = 0; i < texts.size(); i++) {
            texts.get(i).setTextColor(i == 0 ? titleColor : subColor);
        }
    }

    public static void collectTextViews(View v, ArrayList<TextView> out) {
        if (v instanceof TextView) {
            out.add((TextView) v);
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                collectTextViews(g.getChildAt(i), out);
            }
        }
    }

    // ------------------------------------------------------------------
    // The mock switch
    // ------------------------------------------------------------------

    /**
     * Replaces the stock Switch inside a TextCheckCell with MeeroGlassSwitch.
     * The cell's public field is updated too, so setChecked()/getCheckBox()
     * keep driving the new widget. Idempotent: an already-swapped cell is
     * skipped, and when the switches toggle is off the MeeroGlassSwitch
     * draws itself exactly like the stock widget anyway.
     */
    public static void swapSwitch(TextCheckCell cell) {
        final Switch old = cell.checkBox;
        if (old == null || old instanceof MeeroGlassSwitch) {
            return;
        }
        ViewGroup.LayoutParams oldParams = old.getLayoutParams();
        final int index = cell.indexOfChild(old);
        final boolean wasChecked = old.isChecked();
        cell.removeView(old);

        MeeroGlassSwitch glass = new MeeroGlassSwitch(cell.getContext());
        glass.setChecked(wasChecked, false);
        cell.checkBox = glass;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(uic(14, 48)), AndroidUtilities.dp(uic(15, 28)));
        if (oldParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams o = (FrameLayout.LayoutParams) oldParams;
            params.gravity = o.gravity;
            params.setMargins(o.leftMargin, o.topMargin, o.rightMargin, o.bottomMargin);
        }
        cell.addView(glass, Math.min(Math.max(index, 0), cell.getChildCount()), params);
    }

    /**
     * Same swap for TextCheckCell2 (the iOS-style check cell used by the
     * premium-elements and disable-swipe rows) - the user's v132 report #2:
     * those rows kept the stock white switch while every sibling row wore
     * the glass one. The field is public since v132, so setChecked() keeps
     * driving the new widget exactly like the TextCheckCell swap.
     */
    public static void swapSwitch(TextCheckCell2 cell) {
        final Switch old = cell.checkBox;
        if (old == null || old instanceof MeeroGlassSwitch) {
            return;
        }
        ViewGroup.LayoutParams oldParams = old.getLayoutParams();
        final int index = cell.indexOfChild(old);
        final boolean wasChecked = old.isChecked();
        cell.removeView(old);

        MeeroGlassSwitch glass = new MeeroGlassSwitch(cell.getContext());
        glass.setChecked(wasChecked, false);
        cell.checkBox = glass;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(uic(14, 48)), AndroidUtilities.dp(uic(15, 28)));
        if (oldParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams o = (FrameLayout.LayoutParams) oldParams;
            params.gravity = o.gravity;
            params.setMargins(o.leftMargin, o.topMargin, o.rightMargin, o.bottomMargin);
        }
        cell.addView(glass, Math.min(Math.max(index, 0), cell.getChildCount()), params);
    }

    // ------------------------------------------------------------------
    // Press pulse (mock: .row:active { transform: scale(.994) })
    // ------------------------------------------------------------------

    /** Arms (or disarms) the mock's press squash on a row. */
    public static void attachPressPulse(final View v, final boolean enable) {
        if (!enable) {
            if (v.getScaleX() != 1f || v.getScaleY() != 1f) {
                v.animate().cancel();
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
            if (v.getTag(R.id.meeroGlassPulse) != null) {
                v.setOnTouchListener(null);
                v.setTag(R.id.meeroGlassPulse, null);
            }
            return;
        }
        if (v.getTag(R.id.meeroGlassPulse) != null) {
            return; // already armed - rebinding keeps the same listener
        }
        v.setTag(R.id.meeroGlassPulse, Boolean.TRUE);
        v.setOnTouchListener((view, event) -> {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                view.setPivotX(view.getWidth() / 2f);
                view.setPivotY(view.getHeight() / 2f);
                view.animate().cancel();
                view.animate().scaleX(0.988f).scaleY(0.988f)
                        .setDuration(120).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.animate().cancel();
                view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(170).start();
            }
            return false; // never consume - the row's own click/ripple stays intact
        });
    }

    // ------------------------------------------------------------------
    // Entrance stagger (once per position, like the main screen's)
    // ------------------------------------------------------------------

    public static final class Entrance {
        // v130 parity: the mock is rise-by-10px + fade over 450ms with a
        // spring ease and a 45ms per-row stagger (animation: rise .45s
        // var(--ease), animation-delay: i*45ms).
        private static final org.telegram.ui.Components.CubicBezierInterpolator EASE =
                new org.telegram.ui.Components.CubicBezierInterpolator(0.2, 0.9, 0.3, 1.35);
        private int max = -1;

        public void run(View v, int position, boolean glass) {
            if (!glass || position <= max) {
                v.animate().cancel();
                v.setAlpha(1f);
                v.setTranslationY(0f);
                return;
            }
            max = position;
            v.setAlpha(0f);
            v.setTranslationY(AndroidUtilities.dp(uic(10, 10)));
            v.animate().alpha(1f).translationY(0f)
                    .setDuration((long) uic(11, 450))
                    .setInterpolator(EASE)
                    .setStartDelay((long) Math.min(position * uic(12, 45), uic(13, 315)))
                    .start();
        }

        public void reset() {
            max = -1;
        }
    }

    // ------------------------------------------------------------------
    // Legacy-base (BaseNekoSettingsActivity) row skinning
    // ------------------------------------------------------------------

    /** Row types that live inside a glass card (mirrors the card feature). */
    public static boolean isLegacyCardType(int type) {
        return type == BaseNekoSettingsActivity.TYPE_SETTINGS
                || type == BaseNekoSettingsActivity.TYPE_CHECK
                || type == BaseNekoSettingsActivity.TYPE_NOTIFICATION_CHECK
                || type == BaseNekoSettingsActivity.TYPE_DETAIL_SETTINGS
                || type == BaseNekoSettingsActivity.TYPE_TEXT
                || type == BaseNekoSettingsActivity.TYPE_CHECKBOX
                || type == BaseNekoSettingsActivity.TYPE_RADIO
                || type == BaseNekoSettingsActivity.TYPE_ACCOUNT
                || type == BaseNekoSettingsActivity.TYPE_CREATION
                || type == BaseNekoSettingsActivity.TYPE_CHECK2
                || type == BaseNekoSettingsActivity.TYPE_CHECKBOX2;
    }

    /** Card edges from neighbouring view types: headers and gaps break groups. */
    public static int legacyCardPos(RecyclerView.Adapter adapter, int position) {
        final boolean first = position <= 0
                || !isLegacyCardType(adapter.getItemViewType(position - 1));
        final boolean last = position >= adapter.getItemCount() - 1
                || !isLegacyCardType(adapter.getItemViewType(position + 1));
        if (MeeroCore.glassCore()) {
            final int nativePos = MeeroCore.nGlassCardPos(first, last);
            if (nativePos != -1) {
                return nativePos;
            }
        }
        if (first && last) {
            return CARD_SINGLE;
        }
        if (first) {
            return CARD_TOP;
        }
        if (last) {
            return CARD_BOTTOM;
        }
        return CARD_MID;
    }

    /**
     * One bind pass for every legacy-base Meero screen: applies the glass
     * look when enabled or restores the exact stock look when not.
     */
    public static void skinLegacyRow(@NonNull RecyclerView.ViewHolder holder, int position,
                                     RecyclerView.Adapter adapter, boolean glass, Entrance entrance) {
        final View v = holder.itemView;
        if (v == null) {
            return;
        }
        final int type = holder.getItemViewType();
        final boolean header = type == BaseNekoSettingsActivity.TYPE_HEADER;
        final boolean card = isLegacyCardType(type);
        if (glass) {
            if (header) {
                setRowMargins(v, false);
                styleHeaderCell(v, true);
            } else if (card) {
                final int edge = legacyCardPos(adapter, position);
                setRowMargins(v, true);
                // v130: rows joining a row above show the mock's 1px in-card
                // separator, inset from the inline-start edge.
                v.setBackground(MeeroGlassTheme.card(edge == CARD_TOP || edge == CARD_SINGLE,
                        edge == CARD_BOTTOM || edge == CARD_SINGLE,
                        edge == CARD_MID || edge == CARD_BOTTOM));
                tintCellText(v, MeeroGlassTheme.ink(), MeeroGlassTheme.sub());
                styleValueChip(v, true);
                // v130: keep the accent icon tiles (they are applied by the
                // older cards feature, which glass screens now skip).
                if (v instanceof TextCell) {
                    ((TextCell) v).setMeeroIconTile(position);
                    ((TextCell) v).setMeeroDividerEnabled(false);
                }
                if (v instanceof TextCheckCell) {
                    swapSwitch((TextCheckCell) v);
                } else if (v instanceof TextCheckCell2) {
                    swapSwitch((TextCheckCell2) v);
                }
                attachPressPulse(v, true);
            } else {
                // gaps / info footers / previews: let the screen background show
                setRowMargins(v, false);
                v.setBackgroundColor(Color.TRANSPARENT);
                tintCellText(v, MeeroGlassTheme.ink(), MeeroGlassTheme.sub());
            }
        } else {
            setRowMargins(v, false);
            if (header) {
                styleHeaderCell(v, false);
                v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (type == BaseNekoSettingsActivity.TYPE_INFO_PRIVACY) {
                v.setBackground(Theme.getThemedDrawable(v.getContext(), R.drawable.greydivider,
                        Theme.getColor(Theme.key_windowBackgroundGrayShadow)));
                tintCellText(v,
                        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2),
                        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            } else if (!(v instanceof ShadowSectionCell)) {
                v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                tintCellText(v,
                        Theme.getColor(Theme.key_windowBackgroundWhiteBlackText),
                        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            }
            styleValueChip(v, false);
            // undo exactly what the glass pass touched; the legacy cards
            // feature (if on) re-dyes tiles/dividers right after us.
            if (v instanceof TextCell) {
                ((TextCell) v).setMeeroIconTile(-1);
            }
            attachPressPulse(v, false);
        }
        if (entrance != null) {
            entrance.run(v, position, glass);
        }
    }

    // ------------------------------------------------------------------
    // CellGroup-base (BaseNekoXSettingsActivity) row skinning
    //
    // v131: the main MeeroX screen grew its row pass inside the activity;
    // the four remaining newer-base screens (General / Chat / Translator /
    // Experimental) need the exact same look, so the pass moves here next
    // to its legacy sibling. This is a verbatim port of the main screen's
    // binder - same order, same colours, same restore values - and the
    // main screen now calls this too, so the two renders can never drift
    // apart again.
    // ------------------------------------------------------------------

    /**
     * Sections "keep" predicate for cellGroup screens: structural rows
     * (info footers, section shadows, gray spacers, collapse cells) span
     * full width without a section gap; every other row keeps one -
     * identical to what the main MeeroX screen has used since v128.
     */
    public static boolean cellGroupSectionsKeep(View view) {
        return !(view instanceof org.telegram.ui.Cells.TextInfoPrivacyCell
                || view instanceof ShadowSectionCell
                || view instanceof org.telegram.ui.Cells.GraySectionCell
                || view instanceof org.telegram.ui.Cells.CollapseTextCell);
    }

    /**
     * Card edges from the row list: a header opens a card, the next header
     * or a divider closes it (the main screen's rule since v127).
     */
    public static int cellGroupCardPos(CellGroup cellGroup, int position) {
        if (cellGroup == null || position < 0 || position >= cellGroup.rows.size()) {
            return CARD_NONE;
        }
        AbstractConfigCell row = cellGroup.rows.get(position);
        if (row instanceof ConfigCellHeader || row instanceof ConfigCellDivider) {
            return CARD_NONE;
        }
        int start = -1;
        for (int i = position - 1; i >= 0; i--) {
            if (cellGroup.rows.get(i) instanceof ConfigCellHeader) { start = i; break; }
            if (cellGroup.rows.get(i) instanceof ConfigCellDivider) { break; }
        }
        if (start < 0) {
            return CARD_NONE;
        }
        int end = cellGroup.rows.size();
        for (int i = start + 1; i < cellGroup.rows.size(); i++) {
            if (cellGroup.rows.get(i) instanceof ConfigCellHeader
                    || cellGroup.rows.get(i) instanceof ConfigCellDivider) { end = i; break; }
        }
        boolean first = position == start + 1;
        boolean last = position == end - 1;
        if (first && last) return CARD_SINGLE;
        if (first) return CARD_TOP;
        if (last) return CARD_BOTTOM;
        return CARD_MID;
    }

    /**
     * One bind pass for every cellGroup-base screen: applies the glass look
     * when enabled or restores the exact stock look when not. Verbatim port
     * of the main screen's binder (margins always, header styling, glass
     * cards with in-card hairlines, value chips, mock-switch swap, press
     * pulse, entrance stagger; stock restore paints the exact base keys).
     */
    public static void skinCellGroupRow(@NonNull RecyclerView.ViewHolder holder, int position,
                                        CellGroup cellGroup, boolean glass, Entrance entrance) {
        final View v = holder.itemView;
        if (v == null) {
            return;
        }
        final int card = cellGroupCardPos(cellGroup, position);
        setRowMargins(v, card != 0);
        if (glass) {
            if (v instanceof HeaderCell) {
                styleHeaderCell(v, true);
            } else if (card != 0) {
                v.setBackground(MeeroGlassTheme.card(card == CARD_TOP || card == CARD_SINGLE,
                        card == CARD_BOTTOM || card == CARD_SINGLE,
                        card == CARD_MID || card == CARD_BOTTOM));
                tintCellText(v, MeeroGlassTheme.ink(), MeeroGlassTheme.sub());
                styleValueChip(v, true);
                if (v instanceof TextCheckCell) {
                    swapSwitch((TextCheckCell) v);
                } else if (v instanceof TextCheckCell2) {
                    swapSwitch((TextCheckCell2) v);
                }
            } else {
                v.setBackgroundColor(Color.TRANSPARENT);
                tintCellText(v, MeeroGlassTheme.ink(), MeeroGlassTheme.sub());
                // MeeroX v276 (his order: take the other features' switches
                // and put them on the missing buttons): the mock switch swap
                // used to live only inside cards, but a card opens at a
                // ConfigCellHeader - so switch rows that live in NO card kept
                // the stock widget (the collapsible chat-top-strip block for
                // example: its opener is a ConfigCellText, so every sub-row
                // computes card == CARD_NONE). Worse, the swap is one-way,
                // so a recycled view sometimes arrived already swapped and
                // sometimes virgin - a row's look depended on recycler luck,
                // exactly his "exit the app and re-enter until the switch
                // matches the rest". Swap here too: idempotent, and the
                // widget still draws byte-identical to stock when the glass
                // switches toggle is off.
                if (v instanceof TextCheckCell) {
                    swapSwitch((TextCheckCell) v);
                } else if (v instanceof TextCheckCell2) {
                    swapSwitch((TextCheckCell2) v);
                }
            }
        } else {
            v.setBackgroundColor(v instanceof ShadowSectionCell
                    ? Theme.getColor(Theme.key_windowBackgroundGrayShadow)
                    : Theme.getColor(Theme.key_windowBackgroundWhite));
            if (v instanceof HeaderCell) {
                styleHeaderCell(v, false);
            } else {
                tintCellText(v,
                        Theme.getColor(Theme.key_windowBackgroundWhiteBlackText),
                        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            }
            styleValueChip(v, false);
        }
        attachPressPulse(v, glass && card != 0 && !(v instanceof HeaderCell));
        if (entrance != null) {
            entrance.run(v, position, glass);
        }
    }
}
