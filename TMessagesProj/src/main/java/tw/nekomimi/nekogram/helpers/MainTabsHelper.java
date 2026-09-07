package tw.nekomimi.nekogram.helpers;

import org.telegram.ui.MainTabsActivity;

import xyz.nextalone.nagram.NaConfig;

public final class MainTabsHelper {
    public static final int MAIN_TABS_HEIGHT = 56;
    public static final int MAIN_TABS_MARGIN = 6; // MeeroX v255 (his order): reference-matched bottom-bar margins
    public static final int MAIN_TABS_MARGIN_COMPACT = 4;
    public static final int FILTER_TABS_HEIGHT = 36;
    public static final int TAB_WIDTH = 80;
    public static final int TAB_PADDING = 4;

    /**
     * Measurements taken from Telegram-iOS:
     * TabBarComponent lays a control out as 48pt wide with an 8pt gap, and
     * TabBarNode spaces items by 4pt.
     */
    public static final int IOS_TAB_BUTTON = 48;
    public static final int IOS_TAB_GAP = 8;
    public static final int IOS_TAB_SPACING = 4;

    private MainTabsHelper() {
    }

    public static boolean isMainTabsHideTitleStyle() {
        return NaConfig.INSTANCE.getMainTabsHideTitles().Bool();
    }

    public static int getMainTabsHeight() {
        return isMainTabsHideTitleStyle() ? FILTER_TABS_HEIGHT : MAIN_TABS_HEIGHT;
    }

    public static int getMainTabsMargin() {
        return isMainTabsHideTitleStyle() ? MAIN_TABS_MARGIN_COMPACT : MAIN_TABS_MARGIN;
    }

    public static int getMainTabsHeightWithMargins() {
        return getMainTabsHeight() + getMainTabsMargin() * 2;
    }

    public static boolean isContactsTabHidden() {
        return NaConfig.INSTANCE.getMainTabsHideContacts().Bool();
    }

    public static int getChatsPosition() {
        return 0;
    }

    public static int getContactsPosition() {
        return 1;
    }

    public static int getCallsOrSettingsPosition() {
        return isContactsTabHidden() ? 1 : 2;
    }

    public static int getProfilePosition() {
        return isContactsTabHidden() ? 2 : 3;
    }

    public static int getFragmentsCount() {
        return isContactsTabHidden() ? 3 : MainTabsActivity.TABS_COUNT;
    }

    /** MeeroX: the iOS bar carries an extra search entry. */
    public static boolean isMeeroSearchTabEnabled() {
        try {
            return tw.nekomimi.nekogram.NekoConfig.meeroDialogsStyle.Bool();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Buttons inside the bar itself. The MeeroX search pill sits outside it,
     * so it is deliberately not counted here.
     */
    public static int getVisibleTabsCount() {
        return getFragmentsCount();
    }

    public static int getTabsViewWidth() {
        return TAB_WIDTH * getVisibleTabsCount() + (getMainTabsMargin() + TAB_PADDING) * 2;
    }
}
