/*
 * MeeroX v255 — message-menu pack (his sealed order of the interactive
 * preview). Helper class for the ChatActivity hooks:
 *   - config gates (unified scroll / autoscroll / comfortable height /
 *     native blur / compact row)
 *   - system blur-behind for the menu window (Android 12+)
 *   - custom item order engine (saved as a plain list of option ids)
 *   - compact circular quick-actions row
 *   - CappedScrollView used to cap the sheet at half the screen
 */
package tw.nekomimi.nekogram;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class MeeroMsgMenu {

    private MeeroMsgMenu() {}

    /* ---------------- config gates (never crash on early boot) ---------------- */

    public static boolean unifiedOn() {
        try { return NekoConfig.meeroMsgUnified.Bool(); } catch (Throwable t) { return false; }
    }

    public static boolean autoscrollOn() {
        try { return NekoConfig.meeroMsgAutoscroll.Bool(); } catch (Throwable t) { return false; }
    }

    public static boolean comfyOn() {
        try { return NekoConfig.meeroMsgComfy.Bool(); } catch (Throwable t) { return false; }
    }

    public static boolean nativeBlurOn() {
        try { return NekoConfig.meeroMsgNativeBlur.Bool() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S; } catch (Throwable t) { return false; }
    }

    public static boolean compactOn() {
        try { return NekoConfig.meeroMsgCompact.Bool(); } catch (Throwable t) { return false; }
    }

    /** The iOS sheet wrapper is needed when ANY of the three scroll options is on. */
    public static boolean wrapNeeded() {
        return unifiedOn() || comfyOn() || autoscrollOn();
    }

    /* ---------------- native blur behind the popup window ---------------- */

    /**
     * Applies the real system blur-behind to the message-menu popup window.
     * Safe no-op below Android 12 or if the window is not attached yet; any
     * failure leaves the stock background untouched.
     */
    public static void applyNativeBlur(final ActionBarPopupWindow window) {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            final View content = window.getContentView();
            if (content == null) return;
            content.post(() -> {
                try {
                    final View root = content.getRootView();
                    if (root == null) return;
                    final WindowManager.LayoutParams lp = (WindowManager.LayoutParams) root.getLayoutParams();
                    if (lp == null) return;
                    // MeeroX v255.3: FLAG_BLUR_BEHIND / blurBehindRadius exist on
                    // API 31+, but this module's compileSdk predates them (CI
                    // caught the direct field access), so set them defensively.
                    lp.flags |= 0x00020000; // WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    try {
                        java.lang.reflect.Field f = WindowManager.LayoutParams.class.getField("blurBehindRadius");
                        f.setInt(lp, Math.max(AndroidUtilities.dp(14), 28));
                    } catch (Throwable ignore) { /* keep flag only */ }
                    // Gentle dim on top of the blur so items stay readable.
                    lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    lp.dimAmount = 0.30f;
                    WindowManager wm = (WindowManager) root.getContext().getSystemService(Context.WINDOW_SERVICE);
                    if (wm != null) wm.updateViewLayout(root, lp);
                } catch (Throwable ignore) { /* fallback: stock dim/background */ }
            });
        } catch (Throwable ignore) {}
    }

    /* ---------------- compact circular quick actions ---------------- */

    /** Reply / Copy / Forward / Edit / Delete — the primary row set. */
    public static boolean isCompactPrimary(int option) {
        return option == 8 || option == 3 || option == 2 || option == 12 || option == 1;
    }

    /**
     * Builds a horizontal row of circular icon buttons. Each button carries the
     * option id as its tag and fires the given listener.
     */
    public static View buildCompactRow(Context context, ArrayList<Integer> optionIds, ArrayList<Integer> iconRes, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int padV = AndroidUtilities.dp(10);
        int padH = AndroidUtilities.dp(6);
        row.setPadding(padH, padV / 2, padH, padV);
        final int size = AndroidUtilities.dp(46);
        final int gap = AndroidUtilities.dp(8);
        for (int i = 0; i < optionIds.size(); i++) {
            final int option = optionIds.get(i);
            TextView btn = new TextView(context);
            btn.setGravity(Gravity.CENTER);
            btn.setTextSize(18);
            btn.setTag(option);
            btn.setText(emojiFor(option));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0x33FFFFFF);
            btn.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (i > 0) lp.leftMargin = gap;
            row.addView(btn, lp);
            btn.setOnClickListener(listener);
        }
        return row;
    }

    /** Emoji glyph per option so the compact row needs no new drawables. */
    private static String emojiFor(int option) {
        switch (option) {
            case 8:  return "↩";   // reply
            case 3:  return "📋";  // copy
            case 2:  return "↪";   // forward
            case 12: return "✏";   // edit
            case 1:  return "🗑";  // delete
            default: return "•";
        }
    }

    /* ---------------- custom item order ---------------- */

    /** Saved order (option ids, comma-separated). Empty = stock order. */
    public static ArrayList<Integer> loadOrder() {
        ArrayList<Integer> out = new ArrayList<>();
        try {
            String raw = NekoConfig.meeroMsgOrder.String();
            if (raw == null || raw.isEmpty()) return out;
            for (String part : raw.split(",")) {
                try { out.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignore) {}
            }
        } catch (Throwable ignore) {}
        return out;
    }

    public static void saveOrder(ArrayList<Integer> order) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < order.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(order.get(i));
            }
            NekoConfig.meeroMsgOrder.setConfigString(sb.toString());
        } catch (Throwable ignore) {}
    }

    /**
     * Stable partial sort of the menu's three parallel lists (items/options/icons)
     * by the saved order. Options absent from the saved list keep their stock
     * relative order and stay after the ordered ones.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void applyOrder(ArrayList items, ArrayList options, ArrayList icons) {
        try {
            final ArrayList<Integer> order = loadOrder();
            if (order.isEmpty() || options == null || options.size() < 2) return;
            final int n = options.size();
            final ArrayList<Integer> idx = new ArrayList<>(n);
            for (int i = 0; i < n; i++) idx.add(i);
            Collections.sort(idx, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    int ra = rank(options, a, order);
                    int rb = rank(options, b, order);
                    if (ra != rb) return ra < rb ? -1 : 1;
                    return a.compareTo(b);
                }
                private int rank(ArrayList options, int pos, ArrayList<Integer> order) {
                    final Object id = options.get(pos);
                    final int r = order.indexOf(id);
                    return r >= 0 ? r : 10000 + pos;
                }
            });
            permute(items, idx);
            permute(options, idx);
            if (icons != null && icons.size() == n) permute(icons, idx);
        } catch (Throwable ignore) { /* keep stock order on any surprise */ }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void permute(ArrayList list, ArrayList<Integer> idx) {
        final ArrayList copy = new ArrayList(list.size());
        for (int i = 0; i < idx.size(); i++) copy.add(list.get(idx.get(i)));
        list.clear();
        list.addAll(copy);
    }

    /* ---------------- capped scroll view (comfortable height) ---------------- */

    /** ScrollView whose measured height never exceeds a pixel cap. */
    public static class CappedScrollView extends ScrollView {
        private int maxHeightPx = 0;

        public CappedScrollView(Context context) {
            super(context);
            setVerticalScrollBarEnabled(false);
        }

        public void setMaxHeightPx(int px) {
            maxHeightPx = px;
            requestLayout();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (maxHeightPx > 0) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
