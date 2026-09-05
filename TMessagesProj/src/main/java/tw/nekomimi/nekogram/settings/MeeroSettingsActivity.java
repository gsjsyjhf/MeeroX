package tw.nekomimi.nekogram.settings;

import tw.nekomimi.nekogram.MeeroStrings;

import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.ActionBar.Theme.ResourcesProvider;
import org.telegram.ui.Cells.CollapseTextCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.MeeroBubbleStyles;
import tw.nekomimi.nekogram.MeeroGlassSupport;
import tw.nekomimi.nekogram.MeeroGlassTheme;
import tw.nekomimi.nekogram.MeeroJanitor;
import tw.nekomimi.nekogram.MeeroTickStyles;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellSelectBox;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;
import xyz.nextalone.nagram.NaConfig;

/**
 * MeeroX: one screen holding every MeeroX switch, grouped by what it changes.
 *
 * The switches were added one per batch and ended up scattered - eighteen of
 * them in a single unbroken run under one "MeeroX" header in Chat settings,
 * plus the switch style over in General and the back-gesture style in
 * Experimental. Finding "voice waveform" meant scrolling past a dozen
 * unrelated rows, and nothing told the user the three screens were related.
 *
 * This is a presentation change only. Every row below binds to the same
 * static ConfigItem the old rows bind to, so a toggle here writes the same
 * preference the old screen writes and both show the new value the moment
 * they are next drawn. Nothing was added, removed, renamed or re-numbered.
 *
 * The old rows are deliberately left where they are. Removing them would
 * break anyone's muscle memory and, more importantly, would mean editing
 * three working screens to add one - so the risky part of the change would be
 * the part that had nothing to do with the feature.
 *
 * One thing that is NOT safe here: reusing the cell objects from those
 * screens. AbstractConfigCell keeps a single cellGroup field set by
 * bindCellGroup(), so a cell appended to a second group forgets the first,
 * and CellGroup.needSetDivider() then calls rows.indexOf(cell) on the wrong
 * list - that returns -1 and rows.get(0) reads the wrong row, or worse walks
 * off the end. Every cell below is therefore a fresh object; only the
 * ConfigItem behind it is shared, which is exactly the part that should be.
 */
@SuppressLint("RtlHardcoded")
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MeeroSettingsActivity extends BaseNekoXSettingsActivity {

    @Override
    protected RecyclerListView.SelectionAdapter getListAdapter() {
        return listAdapter;
    }

    @Override
    protected CellGroup getCellGroup() {
        return cellGroup;
    }

    @Override
    protected String getSettingsPrefix() {
        return "meerox";
    }

    private final CellGroup cellGroup = new CellGroup(this);

    // Appearance - what an idle screen looks like.
    private final AbstractConfigCell headerAppearance = cellGroup.appendCell(new ConfigCellHeader(MeeroStrings.s(104)));
    // MeeroX v126: the master switch for the fixed "Glass Night" skin of the
    // MeeroX settings screens (ROADMAP batch v126: foundation + chrome).
    // OFF returns the exact stock themed look; no other row cares about it.
    private final AbstractConfigCell glassDesignRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroGlassSettings, MeeroStrings.s(102)));
    // MeeroX v129: mock-accurate switches sit directly under the master
    // design row. Own on/off; OFF = stock switch even under the glass skin.
    private final AbstractConfigCell glassSwitchesRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroGlassSwitches, MeeroStrings.s(103)));
    // MeeroX v125: ONE combined row owns both shape pickers - its name tells
    // the user it holds two features, and the tap opens the shared modern
    // sheet on the bubbles tab (the read-marks tab lives inside the same
    // sheet). The old separate tick-style row is gone.
    private final AbstractConfigCell bubbleStyleRow = cellGroup.appendCell(new ConfigCellSelectBox("MeeroPickerRowTitle", NekoConfig.meeroBubbleStyle, bubbleStyleNames(), () -> showBubbleStyleDialog()));
    private final AbstractConfigCell cardsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroCards, MeeroStrings.s(36)));
    private final AbstractConfigCell dialogsStyleRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroDialogsStyle, MeeroStrings.s(83)));
    private final AbstractConfigCell glassBordersRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroGlassBorders, MeeroStrings.s(101)));
    private final AbstractConfigCell iosShadowsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosShadows, MeeroStrings.s(149)));
    private final AbstractConfigCell iosIconsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosIcons, MeeroStrings.s(137)));
    // Mirrors the General screen's row. The stored value is the option's
    // index, so the four labels have to stay in this order and iOS has to
    // stay last - a different order here would write a value the other screen
    // reads back as a different style.
    private final AbstractConfigCell switchStyleRow = cellGroup.appendCell(new ConfigCellSelectBox("SwitchStyle", NaConfig.INSTANCE.getSwitchStyle(), new String[]{
            getString(R.string.Default),
            getString(R.string.StyleModern),
            getString(R.string.StyleMaterialDesign3),
            MeeroStrings.s(362)
    }, null));
    private final AbstractConfigCell iosRowRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosRow, MeeroStrings.s(146)));
    private final AbstractConfigCell iosStoriesRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosStories, MeeroStrings.s(151)));
    private final AbstractConfigCell iosCallRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosCall, MeeroStrings.s(133)));
    private final AbstractConfigCell iosAlertsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosAlerts, MeeroStrings.s(131)));
    private final AbstractConfigCell iosMediaGridRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosMediaGrid, MeeroStrings.s(142)));
    private final AbstractConfigCell dividerAppearance = cellGroup.appendCell(new ConfigCellDivider());

    // Chat - things that only show up inside a conversation.
    private final AbstractConfigCell headerChat = cellGroup.appendCell(new ConfigCellHeader(MeeroStrings.s(105)));
    private final AbstractConfigCell tapMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroTapMenu, MeeroStrings.s(264)));
    private final AbstractConfigCell menuBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroMenuBlur, MeeroStrings.s(170)));
    // MeeroX v107: separate switch for the full-screen fog behind the
    // bottom-bar chats popup (menuBlur above frosts the menu panel itself).
    private final AbstractConfigCell chatsMenuFogRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroChatsMenuFog, MeeroStrings.s(65)));
    private final AbstractConfigCell iosInputPillRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosInputPill, MeeroStrings.s(138)));
    // MeeroX v142: approved mock "preview-v142" - the iPhone chat header
    // (centered name/status pill + detached photo circle at the edge; tools
    // behind the photo tap / long-press glass menu).
    private final AbstractConfigCell iosWaveformRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosWaveform, MeeroStrings.s(152)));
    private final AbstractConfigCell iosCodeRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosCode, MeeroStrings.s(134)));
    private final AbstractConfigCell iosSelectionRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosSelection, MeeroStrings.s(148)));
    // MeeroX v159: approved polish - true-black AMOLED bubbles + one corner
    // radius for every in-bubble card.
    private final AbstractConfigCell amoledBubblesRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroAmoledBubbles, MeeroStrings.s(8)));
    // MeeroX v164 (approved pick): the AMOLED bubble hairline - defaults OFF
    // so the full-pure-black blend stays for everyone who prefers it merged.
    private final AbstractConfigCell amoledStrokeRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroAmoledStroke, MeeroStrings.s(9)));
    private final AbstractConfigCell unifiedRadiiRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroUnifiedRadii, MeeroStrings.s(267)));
    // MeeroX v92: delivery ticks - a dedicated master switch (off returns the
    // official Android ticks). MeeroX v125: the tick-shape picker row that
    // used to sit beneath it was merged into the single combined row above
    // ("Bubbles & read marks"), whose sheet hosts both pickers as tabs - one
    // row, two features, no duplicates.
    private final AbstractConfigCell ticksSwitchRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroTicksSwitch, MeeroStrings.s(266)));
    private final AbstractConfigCell storyDownloadRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroStoryDownload, MeeroStrings.s(260)));
    // MeeroX v95: the ghost swipe-read toggle moved into GhostModeActivity
    // (circle-style row) so all ghost features live in one place.
    private final AbstractConfigCell dividerChat = cellGroup.appendCell(new ConfigCellDivider());

    // Navigation - moving between screens and lists.
    private final AbstractConfigCell headerNavigation = cellGroup.appendCell(new ConfigCellHeader(MeeroStrings.s(107)));
    private final AbstractConfigCell iosSearchRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosSearch, MeeroStrings.s(147)));
    private final AbstractConfigCell iosFastScrollRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosFastScroll, MeeroStrings.s(135)));
    // Mirrors the Experimental screen's row, index-valued in the same way.
    // Predictive is always listed even below API 34, exactly as it is there -
    // dropping it would shift iOS from 3 to 2 and reinterpret the preference.
    private final AbstractConfigCell backAnimationStyleRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getBackAnimationStyle(), new String[]{
            getString(R.string.BackAnimationClassic),
            getString(R.string.BackAnimationSpring),
            getString(R.string.BackAnimationPredictive),
            MeeroStrings.s(0),
    }, null));
    private final AbstractConfigCell dividerNavigation = cellGroup.appendCell(new ConfigCellDivider());

    // Motion and feedback - how the interface reacts to a touch.
    private final AbstractConfigCell headerMotion = cellGroup.appendCell(new ConfigCellHeader(MeeroStrings.s(106)));
    private final AbstractConfigCell iosAnimRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosAnim, MeeroStrings.s(132)));
    private final AbstractConfigCell iosMenuAnimRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosMenuAnim, MeeroStrings.s(143)));
    private final AbstractConfigCell iosPopupMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosPopupMenu, MeeroStrings.s(145)));
    // v207 (owner orders): the v205 avatar row is REMOVED ("زيله منحتاجه اصلا");
    // in its slot, the menu-diagnostics switch (vault rows 468 title / 469
    // desc, default OFF) - explicit title arg, so the raw-key fallback the
    // owner photographed on the avatar row can never show here.
    private final AbstractConfigCell menuWatchDiagRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroMenuWatchDiag, MeeroStrings.s(469), MeeroStrings.s(468)));
    // v208 (owner-approved preview A): iOS attach-sheet face - vault 470 title / 471 desc
    private final AbstractConfigCell iosAttachPanelRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosAttachPanel, MeeroStrings.s(471), MeeroStrings.s(470)));
    private final AbstractConfigCell iosMsgMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosMsgMenu, MeeroStrings.s(144)));
    private final AbstractConfigCell iosMainMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosMainMenu, MeeroStrings.s(141)));
    // MeeroX v159: approved polish bundle for the menus themselves.
    private final AbstractConfigCell swiftMenusRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroSwiftMenus, MeeroStrings.s(262)));
    private final AbstractConfigCell sepFadeRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroSepFade, MeeroStrings.s(228)));
    private final AbstractConfigCell flexWidthRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroFlexWidth, MeeroStrings.s(92)));
    private final AbstractConfigCell iosHapticsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosHaptics, MeeroStrings.s(136)));
    private final AbstractConfigCell iosLoadingRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosLoading, MeeroStrings.s(140)));
    // MeeroX v164 (approved pick): startup smoothness pre-warm, one shot per
    // launch - lives with the motion rows since its whole job is feel.
    private final AbstractConfigCell smoothPassRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroSmoothPass, MeeroStrings.s(235)));
    // MeeroX v231 (owner pick «خيار 1»): the global 0.75x animation pace.
    // Default ON; OFF returns the stock 1.0x instantly via the callback in
    // the constructor (same pattern NekoChatSettingsActivity uses).
    private final AbstractConfigCell fastAnimationsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroFastAnimations, getString(R.string.MeeroFastAnimTitle)));
    private final AbstractConfigCell dividerMotion = cellGroup.appendCell(new ConfigCellDivider());

    // Storage - the auto cache janitor (MeeroX v159, approved feature). The
    // master switch defaults OFF; the three pickers only shape what an armed
    // janitor does. It deletes re-downloadable cloud-media copies only -
    // never messages, never the database, never music.
    private final AbstractConfigCell headerStorage = cellGroup.appendCell(new ConfigCellHeader(MeeroStrings.s(109)));
    private final AbstractConfigCell autoJanitorRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroAutoJanitor, MeeroStrings.s(16)));
    private final AbstractConfigCell janitorLimitRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NekoConfig.meeroJanitorLimit, MeeroJanitor.limitTitles(), null));
    private final AbstractConfigCell janitorAgeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NekoConfig.meeroJanitorAge, new String[]{
            MeeroStrings.s(3),
            MeeroStrings.s(1),
            MeeroStrings.s(2),
    }, null));
    private final AbstractConfigCell janitorModeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NekoConfig.meeroJanitorMode, new String[]{
            MeeroStrings.s(4),
            MeeroStrings.s(6),
            MeeroStrings.s(5),
    }, null));
    private final AbstractConfigCell dividerStorage = cellGroup.appendCell(new ConfigCellDivider());

    // Sound and launch.
    private final AbstractConfigCell headerSound = cellGroup.appendCell(new ConfigCellHeader(MeeroStrings.s(108)));
    private final AbstractConfigCell iosSoundsRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosSounds, MeeroStrings.s(150)));
    private final AbstractConfigCell iosIntroRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.meeroIosIntro, MeeroStrings.s(139)));
    private final AbstractConfigCell dividerSound = cellGroup.appendCell(new ConfigCellDivider());

    private ListAdapter listAdapter;

    public MeeroSettingsActivity() {
        // MeeroX v231: instant-apply the fast-motion switch (standard
        // cellGroup callback; cells are field-initialized before this runs).
        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            if (NekoConfig.meeroFastAnimations.getKey().equals(key)) {
                tw.nekomimi.nekogram.MeeroFastMotion.apply();
            }
        };
        addRowsToMap(cellGroup);
    }

    @Override
    public View createView(Context context) {
        View superView = super.createView(context);

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        setupDefaultListeners();
        applyMeeroGlassChrome();
        applySectionsSkin();

        return superView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // v194: owner's channel-subscribe prompt - every entry until the
        // join button is tapped once (non-cancelable by explicit order);
        // never-throw helper, see MeeroChannelPromo.
        tw.nekomimi.nekogram.MeeroChannelPromo.maybeShow(this);
    }

    /**
     * MeeroX v128 FIX: the base fragment called listView.setSections(true),
     * which attaches a decoration that paints THEMED section cards
     * (key_windowBackgroundWhite - a blue-gray in many dark themes) behind
     * the rows. Live that looks fine at rest, but while scrolling the
     * sections drawer repaints and the themed wash bleeds through our
     * translucent cards: the whole list flashed to image #2 in the user's
     * report. The same call also re-arms the themed row selector
     * (key_settings_listSelector).
     *
     * While the glass switch is on we re-install the SAME geometry
     * (12dp/16dp/topPadding) but with a painter that paints NOTHING - our
     * v127 cards are the only cards in town - and pin the row selector to
     * our fixed press tint. OFF restores the exact stock call.
     */
    private void applySectionsSkin() {
        // v129: shared with the legacy sub-screens (MeeroGlassSupport).
        MeeroGlassSupport.applySectionsSkin(listView, glassOn(),
                view -> !(view instanceof TextInfoPrivacyCell
                        || view instanceof ShadowSectionCell
                        || view instanceof GraySectionCell
                        || view instanceof CollapseTextCell));
    }

    // ------------------------------------------------------------------
    // MeeroX v126: fixed "Glass Night" chrome (batch v126 = foundation).
    //
    // HOW "theme-proof" works here, in three layers, without touching any
    // Telegram or NagramX rendering code:
    //  1. getResourceProvider(): while the glass switch is on we answer
    //     every theme-key lookup from the fixed MeeroGlassTheme palette, so
    //     even Telegram's own adaptive ActionBar (which animates between
    //     two theme keys on scroll) paints OUR fixed colors automatically.
    //  2. getThemeDescriptions(): with the glass on we return an empty
    //     list, so Telegram's theme-reload machinery simply has nothing to
    //     repaint on this screen - "not affected by themes" is structural,
    //     not a race against repaints. OFF returns the stock list verbatim.
    //  3. onBindMeeroGlass(): every row bind either applies the glass look
    //     or restores the exact stock colors, so the master switch flips
    //     live with a simple notifyDataSetChanged().
    // ------------------------------------------------------------------

    private boolean glassOn() {
        // v127: single source of truth lives in MeeroGlassTheme so the cell
        // providers and this screen always agree about the toggle state.
        return MeeroGlassTheme.enabled();
    }

    @Override
    public ResourcesProvider getResourceProvider() {
        ResourcesProvider base = super.getResourceProvider();
        return glassOn() ? MeeroGlassTheme.wrap(base) : base;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        if (glassOn()) {
            return new ArrayList<>();
        }
        return super.getThemeDescriptions();
    }

    private void applyMeeroGlassChrome() {
        if (fragmentView == null) {
            return;
        }
        if (glassOn()) {
            fragmentView.setBackground(MeeroGlassTheme.screenBackground());
        } else {
            fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        }
    }

    @Override
    protected void handleCellClick(View view, int position, float x, float y) {
        super.handleCellClick(view, position, x, y);
        if (position >= 0 && position < cellGroup.rows.size()
                && cellGroup.rows.get(position) == glassDesignRow) {
            // Live flip: palette + cells repaint right now. Everything reads
            // glassOn() lazily, so the provider and future binds follow the
            // new value by themselves; we only nudge the eager parts.
            if (glassOn()) {
                fragmentView.setBackground(MeeroGlassTheme.screenBackground());
                actionBar.setBackgroundColor(MeeroGlassTheme.actionBarBg());
                actionBar.setTitleColor(MeeroGlassTheme.ink());
                actionBar.setItemsColor(MeeroGlassTheme.ink(), false);
            }
            applySectionsSkin();
            if (!glassOn()) {
                // exact stock, mirroring the base fragment's theme keys
                fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
                actionBar.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
                actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
                actionBar.setItemsColor(Theme.getColor(Theme.key_avatar_actionBarIconBlue), false);
            }
            glassEntrance.reset(); // replay the entrance under the new look
            listAdapter.notifyDataSetChanged();
        }
        if (position >= 0 && position < cellGroup.rows.size()
                && cellGroup.rows.get(position) == glassSwitchesRow) {
            // Redraw-only toggle: swapped switches repaint per frame from
            // the live flag, stock cells are untouched rows. Just rebind.
            listAdapter.notifyDataSetChanged();
        }
    }

    /** Applies the glass look - or restores exact stock - on every row bind. */
    private void onBindMeeroGlass(@NonNull RecyclerView.ViewHolder holder, int position) {
        // v131: the pass itself moved to MeeroGlassSupport.skinCellGroupRow
        // (verbatim port) so the four remaining newer-base screens render
        // through the very same code; this screen keeps only its per-screen
        // entrance state and toggle.
        MeeroGlassSupport.skinCellGroupRow(holder, position, cellGroup, glassOn(), glassEntrance);
    }

    // v129: the row skinning helpers (margins, header style, value chips,
    // text tinting, the entrance stagger) moved to MeeroGlassSupport so the
    // fourteen legacy-base Meero sub-screens replay the exact same look;
    // this screen keeps only its per-screen entrance state.
    private final MeeroGlassSupport.Entrance glassEntrance = new MeeroGlassSupport.Entrance();

    static String tickStyleName(int style) {
        switch (style) {
            case 1:  return MeeroStrings.s(438);
            case 2:  return MeeroStrings.s(445);
            case 3:  return MeeroStrings.s(446);
            case 4:  return MeeroStrings.s(447);
            case 5:  return MeeroStrings.s(448);
            case 6:  return MeeroStrings.s(449);
            case 7:  return MeeroStrings.s(450);
            case 8:  return MeeroStrings.s(451);
            case 9:  return MeeroStrings.s(452);
            case 10: return MeeroStrings.s(439);
            case 11: return MeeroStrings.s(440);
            case 12: return MeeroStrings.s(441);
            case 13: return MeeroStrings.s(442);
            case 14: return MeeroStrings.s(443);
            case 15: return MeeroStrings.s(444);
            default: return MeeroStrings.s(437);
        }
    }

    static String tickStyleDesc(int style) {
        switch (style) {
            case 1:  return MeeroStrings.s(422);
            case 2:  return MeeroStrings.s(429);
            case 3:  return MeeroStrings.s(430);
            case 4:  return MeeroStrings.s(431);
            case 5:  return MeeroStrings.s(432);
            case 6:  return MeeroStrings.s(433);
            case 7:  return MeeroStrings.s(434);
            case 8:  return MeeroStrings.s(435);
            case 9:  return MeeroStrings.s(436);
            case 10: return MeeroStrings.s(423);
            case 11: return MeeroStrings.s(424);
            case 12: return MeeroStrings.s(425);
            case 13: return MeeroStrings.s(426);
            case 14: return MeeroStrings.s(427);
            case 15: return MeeroStrings.s(428);
            default: return MeeroStrings.s(421);
        }
    }

    /**
     * Draws one tick mark for the picker dialog, tinted like dialog text so
     * the preview reads on any theme. "second" picks the mark that joins the
     * first on a read receipt, exactly as the cell layers them.
     */
    static Drawable tickStyleIcon(Context context, int style, boolean second) {
        if (style < 0 || style >= MeeroTickStyles.COUNT) {
            style = 0;
        }
        int res = (second ? MeeroTickStyles.SECONDS : MeeroTickStyles.SINGLES)[style];
        Drawable icon = context.getResources().getDrawable(res).mutate();
        icon.setColorFilter(Theme.getColor(Theme.key_dialogTextBlack), PorterDuff.Mode.SRC_IN);
        return icon;
    }

    // MeeroX v122: bubble shapes. Mirrors the tick picker one-to-one so the
    // two selectors behave identically; the only difference is the preview -
    // here each row draws the actual bubble outline it would apply.
    private static final int BUBBLE_STYLE_COUNT = MeeroBubbleStyles.COUNT;

    static String bubbleStyleName(int style) {
        switch (style) {
            case 1:  return MeeroStrings.s(375);
            case 2:  return MeeroStrings.s(376);
            case 3:  return MeeroStrings.s(377);
            case 4:  return MeeroStrings.s(378);
            // MeeroX v124: the three shapes added with the picker sheet.
            case 5:  return MeeroStrings.s(379);
            case 6:  return MeeroStrings.s(380);
            case 7:  return MeeroStrings.s(381);
            default: return MeeroStrings.s(374);
        }
    }

    static String bubbleStyleDesc(int style) {
        switch (style) {
            case 1:  return MeeroStrings.s(367);
            case 2:  return MeeroStrings.s(368);
            case 3:  return MeeroStrings.s(369);
            case 4:  return MeeroStrings.s(370);
            case 5:  return MeeroStrings.s(371);
            case 6:  return MeeroStrings.s(372);
            case 7:  return MeeroStrings.s(373);
            default: return MeeroStrings.s(366);
        }
    }

    private static String[] bubbleStyleNames() {
        String[] names = new String[BUBBLE_STYLE_COUNT];
        for (int i = 0; i < BUBBLE_STYLE_COUNT; i++) {
            names[i] = bubbleStyleName(i);
        }
        return names;
    }

    private void showBubbleStyleDialog() {
        // MeeroX v124: the old AlertDialog list became the modern shared
        // bottom sheet (design A) - same skin for both pickers, tab #0.
        MeeroPickerSheet.open(getParentActivity(), MeeroPickerSheet.TAB_BUBBLES, () -> {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public String getTitle() {
        return MeeroStrings.s(230);
    }

    @Override
    public int getDrawable() {
        return R.drawable.msg_photo_settings_solar;
    }

    /**
     * MeeroX: base id for this screen's search entries.
     *
     * SettingsHelper builds a search result's guid as getBaseGuid() + the row
     * index, so two screens whose bases are closer together than their row
     * counts would hand out the same guid for different rows. The existing
     * screens sit at 10000, 11000, 12000 and 13000; 14000 continues that
     * spacing and leaves this screen the whole block to itself.
     */
    @Override
    public int getBaseGuid() {
        return 14000;
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        protected View createDefaultViewByType(int viewType) {
            // v127: the standard cells, but born with the fixed MeeroX cell
            // palette - the provider is live, so it also serves the stock
            // look verbatim whenever the glass switch is off.
            if (viewType == CellGroup.ITEM_TYPE_TEXT_CHECK) {
                return new TextCheckCell(mContext, 21, false, MeeroGlassTheme.cells());
            }
            if (viewType == CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL) {
                return new TextSettingsCell(mContext, MeeroGlassTheme.cells());
            }
            if (viewType == CellGroup.ITEM_TYPE_HEADER) {
                return new HeaderCell(mContext, MeeroGlassTheme.cells());
            }
            if (viewType == CellGroup.ITEM_TYPE_TEXT_CHECK_ICON) {
                return new TextCell(mContext, 23, false, true, MeeroGlassTheme.cells());
            }
            return super.createDefaultViewByType(viewType);
        }

        @Override
        protected void onBindDefaultViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            // v126/v127: glass chrome / stock restore per row, card layout,
            // value chips and the entrance stagger (see onBindMeeroGlass)
            onBindMeeroGlass(holder, position);
        }
    }
}
