/*
 * MeeroX v255 — custom message-menu order page (his order).
 * Simple and safe on purpose: each known message-menu action is listed in the
 * current saved order and moved with up/down buttons; the list is applied to
 * the real long-press menu immediately (MeeroMsgMenu.applyOrder).
 */
package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Collections;

import tw.nekomimi.nekogram.MeeroMsgMenu;

public class MeeroMsgMenuOrderActivity extends BaseFragment {

    /* Known actions (matching ChatActivity.OPTION_* ids) with their strings. */
    private static final int[] OPT_IDS = {
        8,   // OPTION_REPLY
        3,   // OPTION_COPY
        2,   // OPTION_FORWARD
        12,  // OPTION_EDIT
        13,  // OPTION_PIN
        22,  // OPTION_COPY_LINK
        4,   // OPTION_SAVE_TO_GALLERY
        104, // OPTION_OPEN_PROFILE
        23,  // OPTION_REPORT_CHAT
        1    // OPTION_DELETE
    };
    private static final int[] OPT_STR = {
        R.string.Reply,
        R.string.Copy,
        R.string.Forward,
        R.string.Edit,
        R.string.PinMessage,
        R.string.CopyLink,
        R.string.SaveToGallery,
        R.string.OpenProfile,
        R.string.ReportChat,
        R.string.Delete
    };

    private LinearLayout rowsContainer;
    private final ArrayList<Integer> order = new ArrayList<>();

    private CharSequence labelFor(int option) {
        for (int i = 0; i < OPT_IDS.length; i++) {
            if (OPT_IDS[i] == option) return LocaleController.getString(OPT_STR[i]);
        }
        return "";
    }

    private CharSequence fallbackLabel(int option) {
        switch (option) {
            case 8: return "رد";
            case 3: return "نسخ";
            case 2: return "توجيه";
            case 12: return "تعديل";
            case 13: return "تثبيت";
            case 22: return "نسخ الرابط";
            case 4: return "حفظ بالمعرض";
            case 104: return "فتح الملف";
            case 23: return "إبلاغ";
            case 1: return "حذف";
            default: return "عنصر";
        }
    }

    @Override
    public View createView(Context context) {
        actionBar = createActionBar(context);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("ترتيب عناصر القائمة");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenu.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        // Load saved order; unknown/new options keep the catalog order.
        ArrayList<Integer> saved = MeeroMsgMenu.loadOrder();
        for (int id : OPT_IDS) {
            if (saved.contains(id)) order.add(id);
        }
        for (int id : OPT_IDS) {
            if (!order.contains(id)) order.add(id);
        }

        android.widget.FrameLayout content = new android.widget.FrameLayout(context);
        content.addView(actionBar, org.telegram.ui.Components.LayoutHelper.createFrame(
                org.telegram.ui.Components.LayoutHelper.MATCH_PARENT,
                org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        rowsContainer = new LinearLayout(context);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        int side = AndroidUtilities.dp(14);
        rowsContainer.setPadding(side, AndroidUtilities.dp(10), side, AndroidUtilities.dp(18));
        scroll.addView(rowsContainer);
        content.addView(scroll, org.telegram.ui.Components.LayoutHelper.createFrame(
                org.telegram.ui.Components.LayoutHelper.MATCH_PARENT,
                org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, ActionBar.getCurrentActionBarHeight() / getResources().getDisplayMetrics().density + 4, 0, 0));

        TextView hint = new TextView(context);
        hint.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        hint.setTextSize(12);
        hint.setPadding(0, 0, 0, AndroidUtilities.dp(10));
        hint.setText("بدّل ترتيب عناصر قائمة الرسالة (الضغطة المطولة) — يتطبق فوراً:");
        rowsContainer.addView(hint);

        rebuild();
        fragmentView = content;
        return content;
    }

    private void rebuild() {
        final Context context = rowsContainer.getContext();
        // keep the hint (first child)
        while (rowsContainer.getChildCount() > 1) rowsContainer.removeViewAt(1);
        for (int i = 0; i < order.size(); i++) {
            final int pos = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));

            TextView name = new TextView(context);
            name.setTextSize(15);
            name.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            CharSequence label = labelFor(order.get(i));
            if (label == null || label.length() == 0) label = fallbackLabel(order.get(i));
            name.setText(label);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView num = new TextView(context);
            num.setTextSize(12);
            num.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            num.setText(String.valueOf(i + 1));
            num.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(4), 0);
            row.addView(num);

            TextView up = button(context, "▲");
            TextView down = button(context, "▼");
            up.setEnabled(pos > 0);
            down.setEnabled(pos < order.size() - 1);
            up.setAlpha(pos > 0 ? 1f : 0.3f);
            down.setAlpha(pos < order.size() - 1 ? 1f : 0.3f);
            up.setOnClickListener(v -> { Collections.swap(order, pos, pos - 1); persistAndRebuild(); });
            down.setOnClickListener(v -> { Collections.swap(order, pos, pos + 1); persistAndRebuild(); });
            row.addView(up);
            row.addView(down);

            rowsContainer.addView(row);
        }
    }

    private TextView button(Context context, String glyph) {
        TextView b = new TextView(context);
        b.setText(glyph);
        b.setTextSize(16);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
        int s = AndroidUtilities.dp(34);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.leftMargin = AndroidUtilities.dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    private void persistAndRebuild() {
        MeeroMsgMenu.saveOrder(order);
        rebuild();
    }
}
