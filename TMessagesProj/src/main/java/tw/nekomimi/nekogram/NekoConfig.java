package tw.nekomimi.nekogram;

import static tw.nekomimi.nekogram.config.ConfigItem.configTypeBool;
import static tw.nekomimi.nekogram.config.ConfigItem.configTypeFloat;
import static tw.nekomimi.nekogram.config.ConfigItem.configTypeInt;
import static tw.nekomimi.nekogram.config.ConfigItem.configTypeLong;
import static tw.nekomimi.nekogram.config.ConfigItem.configTypeMapIntInt;
import static tw.nekomimi.nekogram.config.ConfigItem.configTypeSetInt;
import static tw.nekomimi.nekogram.config.ConfigItem.configTypeString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Pair;

import com.radolyn.ayugram.utils.AyuGhostUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tw.nekomimi.nekogram.config.ConfigItem;
import tw.nekomimi.nekogram.helpers.CloudSettingsHelper;

@SuppressLint("ApplySharedPref")
@SuppressWarnings("unused")
public class NekoConfig {

    public static final int TABLET_AUTO = 0;
    public static final int TABLET_ENABLE = 1;

    public static final int DIALOG_FILTER_EXCLUDE_NONE = 0;
    public static final int DIALOG_FILTER_EXCLUDE_MUTED = 1;
    public static final int DIALOG_FILTER_EXCLUDE_ALL = 2;

    public static final int MARKDOWN_PARSER_TELEGRAM = 0;
    public static final int MARKDOWN_PARSER_NEKO = 1;

    public static final int DRAWER_BACKGROUND_DEFAULT = 0;
    public static final int DRAWER_BACKGROUND_AVATAR = 1;
    public static final int DRAWER_BACKGROUND_BIG_AVATAR = 2;
    public static final int DRAWER_BACKGROUND_WALLPAPER = 3;

    public static final int DNS_TYPE_DEFAULT = 0;
    public static final int DNS_TYPE_NAX = 1;
    public static final int DNS_TYPE_SYSTEM = 2;
    public static final int DNS_TYPE_CUSTOM_DOH = 3;

    public static final int ID_TYPE_HIDDEN = 0;
    public static final int ID_TYPE_API = 1;
    public static final int ID_TYPE_BOT_API = 2;

    private static SharedPreferences preferences;

    public static SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("nkmrcfg", Context.MODE_PRIVATE);
        }
        return preferences;
    }

    public static final Object sync = new Object();

    private static boolean configLoaded = false;
    private static final ArrayList<ConfigItem> configs = new ArrayList<>();
    public static final ArrayList<DatacenterInfo> datacenterInfos = new ArrayList<>(5);

    // Configs
    public static ConfigItem unreadBadgeOnBackButton = addConfig("unreadBadgeOnBackButton", configTypeBool, false);
    public static ConfigItem useCustomEmoji = addConfig("useCustomEmoji", configTypeBool, false);
    public static ConfigItem repeatConfirm = addConfig("repeatConfirm", configTypeBool, true);
    public static ConfigItem disableInstantCamera = addConfig("DisableInstantCamera", configTypeBool, true);
    public static ConfigItem showSeconds = addConfig("showSeconds", configTypeBool, false);

    // MeeroX: iOS 26 chat styling. Both default to on; turning them off
    // restores stock Telegram behaviour exactly.
    public static ConfigItem meeroMenuBlur = addConfig("meeroMenuBlur", configTypeBool, true);
    // MeeroX v122: the iOS-bubble on/off switch became the meeroBubbleStyle
    // picker below; this bool stays only so first launch after the update can
    // migrate the old value (loadConfig below). No UI references it anymore.
    public static ConfigItem meeroIosBubbles = addConfig("meeroIosBubbles", configTypeBool, true);
    public static ConfigItem meeroBubbleStyle = addConfig("meeroBubbleStyle", configTypeInt, 1);
    public static ConfigItem meeroTapMenu = addConfig("meeroTapMenu", configTypeBool, true);
    // MeeroX v126: fixed exclusive "Glass Night" skin for MeeroX settings
    // screens - ignores Telegram themes, follows day/night only. Default ON
    // (display feature); OFF returns the stock themed look exactly.
    public static ConfigItem meeroGlassSettings = addConfig("meeroGlassSettings", configTypeBool, true);
    // MeeroX v129: switches drawn exactly like the preview mock (gradient
    // track, glow, stretching knob). Default ON (display feature); OFF
    // returns the stock Switch look even while the glass design stays on.
    public static ConfigItem meeroGlassSwitches = addConfig("meeroGlassSwitches", configTypeBool, true);
    public static ConfigItem meeroIosAnim = addConfig("meeroIosAnim", configTypeBool, true);
    public static ConfigItem meeroCards = addConfig("meeroCards", configTypeBool, true);
    public static ConfigItem meeroDialogsStyle = addConfig("meeroDialogsStyle", configTypeBool, true);
    public static ConfigItem meeroIosIcons = addConfig("meeroIosIcons", configTypeBool, true);
    public static ConfigItem meeroIosSounds = addConfig("meeroIosSounds", configTypeBool, true);
    public static ConfigItem meeroGlassBorders = addConfig("meeroGlassBorders", configTypeBool, true);
    public static ConfigItem meeroIosLoading = addConfig("meeroIosLoading", configTypeBool, true);
    public static ConfigItem meeroIosCode = addConfig("meeroIosCode", configTypeBool, true);
    public static ConfigItem meeroIosIntro = addConfig("meeroIosIntro", configTypeBool, true);
    // v207 (owner order: "زيله منحتاجه اصلا"): the v205 meeroChatHeaderAvatar
    // switch is REMOVED - key, settings row and ChatAvatarContainer hook.
    public static ConfigItem meeroIosSearch = addConfig("meeroIosSearch", configTypeBool, true);
    public static ConfigItem meeroIosFastScroll = addConfig("meeroIosFastScroll", configTypeBool, true);
    public static ConfigItem meeroIosShadows = addConfig("meeroIosShadows", configTypeBool, true);
    public static ConfigItem meeroIosHaptics = addConfig("meeroIosHaptics", configTypeBool, true);
    public static ConfigItem meeroIosMenuAnim = addConfig("meeroIosMenuAnim", configTypeBool, true);
    public static ConfigItem meeroIosPopupMenu = addConfig("meeroIosPopupMenu", configTypeBool, true);
    // v207 (owner order): menu-diagnostics capture is opt-in and OFF by
    // default - a user with a misbehaving menu flips this on, reproduces,
    // and pastes the auto-copied technical report. No timers, no toasts and
    // no clipboard writes while it is off.
    public static ConfigItem meeroMenuWatchDiag = addConfig("meeroMenuWatchDiag", configTypeBool, false);
    // v208 (owner-approved preview): iOS face for the attach sheet - grabber
    // + grouped squircle action card. OFF = stock tab strip, byte-exact.
    public static ConfigItem meeroIosAttachPanel = addConfig("meeroIosAttachPanel", configTypeBool, true);
    // MeeroX v230: send text style picker (0 default, 1..8 TG entities).
    public static ConfigItem meeroSendTextStyle = addConfig("meeroSendTextStyle", configTypeInt, 0);
    // MeeroX v231: global 0.75x animation pace (default ON - owner's pick).
    public static ConfigItem meeroFastAnimations = addConfig("meeroFastAnimations", configTypeBool, true);
    public static ConfigItem meeroIosMsgMenu = addConfig("meeroIosMsgMenu", configTypeBool, true);
    public static ConfigItem meeroIosMainMenu = addConfig("meeroIosMainMenu", configTypeBool, true);
    // MeeroX v159: the approved polish bundle (each off = exact v158 look).
    public static ConfigItem meeroSwiftMenus = addConfig("meeroSwiftMenus", configTypeBool, true);
    public static ConfigItem meeroSepFade = addConfig("meeroSepFade", configTypeBool, true);
    public static ConfigItem meeroFlexWidth = addConfig("meeroFlexWidth", configTypeBool, true);
    public static ConfigItem meeroAmoledBubbles = addConfig("meeroAmoledBubbles", configTypeBool, true);
    public static ConfigItem meeroUnifiedRadii = addConfig("meeroUnifiedRadii", configTypeBool, true);
    // MeeroX v164 (his two approved picks from preview-ideas-v164):
    // 1. AMOLED bubble edge - a faint 1px hairline around pure-black
    //    incoming bubbles so they separate from true-black backgrounds.
    //    Defaults OFF: the full-pure-black blend stays untouched for
    //    everyone who prefers it merged (existing v159 behaviour).
    public static ConfigItem meeroAmoledStroke = addConfig("meeroAmoledStroke", configTypeBool, false);
    // 2. Smooth-start pack - one-shot pre-warm of the first popup menu,
    //    first chat open and first chat-list swipe after launch. OFF =
    //    startup byte-identical to v163 (nothing is even scheduled).
    public static ConfigItem meeroSmoothPass = addConfig("meeroSmoothPass", configTypeBool, true);
    // MeeroX v159: Auto Janitor - scheduled, size-capped cache cleaning.
    // The master switch defaults OFF: it deletes files, so it arms only by
    // explicit user choice. Media files removed here are re-downloadable
    // cloud copies; messages and the database are never touched.
    public static ConfigItem meeroAutoJanitor = addConfig("meeroAutoJanitor", configTypeBool, false);
    // Index into {1, 2, 4, 8, 16} GB - default 8 GB.
    public static ConfigItem meeroJanitorLimit = addConfig("meeroJanitorLimit", configTypeInt, 3);
    // Index into {7, 14, 30} days - default 14.
    public static ConfigItem meeroJanitorAge = addConfig("meeroJanitorAge", configTypeInt, 1);
    // 0 = daily, 1 = weekly, 2 = only when over the limit. Default weekly.
    public static ConfigItem meeroJanitorMode = addConfig("meeroJanitorMode", configTypeInt, 1);
    public static ConfigItem meeroJanitorLastRun = addConfig("meeroJanitorLastRun", configTypeLong, 0L);
    // Bytes freed by the last automatic pass, shown as the "freed X" report.
    public static ConfigItem meeroJanitorFreed = addConfig("meeroJanitorFreed", configTypeLong, 0L);
    // MeeroX v161: Theme Mixer composition state (indices into
    // MeeroThemeMixer.accents()/backgrounds() + the incoming-bubble choice).
    public static ConfigItem meeroMixerAccent = addConfig("meeroMixerAccent", configTypeInt, 1);
    public static ConfigItem meeroMixerBg = addConfig("meeroMixerBg", configTypeInt, 0);
    public static ConfigItem meeroMixerInBubble = addConfig("meeroMixerInBubble", configTypeInt, 0);
    public static ConfigItem meeroIosInputPill = addConfig("meeroIosInputPill", configTypeBool, true);
    // MeeroX v244 (his approved preview v3: H~45 W=165):
    // iOS name capsule in the chat glass header - fixed dp(165) width,
    // long titles marquee inside; OFF = the stock full-width capsule.
    public static ConfigItem meeroIosCapsule = addConfig("meeroIosCapsule", configTypeBool, false);
    public static ConfigItem meeroIosWaveform = addConfig("meeroIosWaveform", configTypeBool, true);
    public static ConfigItem meeroIosSelection = addConfig("meeroIosSelection", configTypeBool, true);
    public static ConfigItem meeroIosRow = addConfig("meeroIosRow", configTypeBool, true);
    public static ConfigItem meeroIosStories = addConfig("meeroIosStories", configTypeBool, true);
    public static ConfigItem meeroIosCall = addConfig("meeroIosCall", configTypeBool, true);
    public static ConfigItem meeroIosAlerts = addConfig("meeroIosAlerts", configTypeBool, true);
    public static ConfigItem meeroIosMediaGrid = addConfig("meeroIosMediaGrid", configTypeBool, true);
    // MeeroX v92: delivery ticks - master switch (off = official Android
    // Telegram ticks) and the chosen shape (0 = the original iOS pair).
    public static ConfigItem meeroTicksSwitch = addConfig("meeroTicksSwitch", configTypeBool, true);
    public static ConfigItem meeroTickStyle = addConfig("meeroTickStyle", configTypeInt, 0);
    // MeeroX v94: save stories to the gallery even when their owner forbids
    // saving, and ghost-mode selective read by swiping a chat in the list.
    // Both off = stock fork behaviour, untouched.
    public static ConfigItem meeroStoryDownload = addConfig("meeroStoryDownload", configTypeBool, true);
    public static ConfigItem meeroGhostSwipeRead = addConfig("meeroGhostSwipeRead", configTypeBool, true);
    // --- MeeroX Auto-reply (v98). Master switch defaults OFF on purpose:
    // it sends messages under the user's name, so the first activation is
    // an explicit user action. Text empty = localized default with {name}.
    public static ConfigItem meeroAutoReply = addConfig("meeroAutoReply", configTypeBool, false);
    public static ConfigItem meeroAutoReplyText = addConfig("meeroAutoReplyText", configTypeString, "");
    public static ConfigItem meeroAutoReplyCooldown = addConfig("meeroAutoReplyCooldown", configTypeInt, 0); // minutes per chat (0 = every message)
    public static ConfigItem meeroAutoReplyDelay = addConfig("meeroAutoReplyDelay", configTypeInt, 3); // seconds before send
    public static ConfigItem meeroAutoReplyRules = addConfig("meeroAutoReplyRules", configTypeString, ""); // v99: JSON per-chat reply rules
    public static ConfigItem meeroAutoReplyExclusions = addConfig("meeroAutoReplyExclusions", configTypeString, ""); // v101: JSON excluded chat ids
    public static ConfigItem meeroActivityStats = addConfig("meeroActivityStats", configTypeBool, true); // v102: activity details + open counter
    public static ConfigItem meeroStatsOpens = addConfig("meeroStatsOpens", configTypeInt, 0); // v102: app opens since v102
    public static ConfigItem meeroStatsSince = addConfig("meeroStatsSince", configTypeInt, 0); // v102: epoch sec of first tracked open
    public static ConfigItem meeroWatchEnabled = addConfig("meeroWatchEnabled", configTypeBool, true); // v102: account watching master
    public static ConfigItem meeroWatchList = addConfig("meeroWatchList", configTypeString, ""); // v102: JSON [{id,on}]
    public static ConfigItem meeroWatchData = addConfig("meeroWatchData", configTypeString, ""); // v102: JSON snapshots {id:{...}}
    public static ConfigItem meeroWatchLog = addConfig("meeroWatchLog", configTypeString, ""); // v102: JSON change log (newest first, cap 150)
    public static ConfigItem meeroDeleteHunter = addConfig("meeroDeleteHunter", configTypeBool, true); // v103: notify+log on delete/edit by others
    public static ConfigItem meeroDeleteLog = addConfig("meeroDeleteLog", configTypeString, ""); // v103: JSON hunter log (newest first, cap 150)
    // --- v185 (batch 2C): seed-sealed native stores for the radar group.
    // Opaque MAC-ed ciphertext blobs authored by libmeerocore; Java never
    // parses them. Legacy JSON keys above stay as the degraded fallback and
    // are imported+cleared on first native run. ---
    public static ConfigItem meeroDeleteStore = addConfig("meeroDeleteStore", configTypeString, "");
    public static ConfigItem meeroWatchStore = addConfig("meeroWatchStore", configTypeString, "");
    public static ConfigItem meeroAutoReplyPoolOn = addConfig("meeroAutoReplyPoolOn", configTypeBool, false); // v103: random reply texts
    public static ConfigItem meeroAutoReplyPool = addConfig("meeroAutoReplyPool", configTypeString, ""); // v103: JSON array of reply texts
    public static ConfigItem meeroAutoReplyRandomEmoji = addConfig("meeroAutoReplyRandomEmoji", configTypeBool, false); // v103: random emoji suffix
    public static ConfigItem meeroAutoReplyWindow = addConfig("meeroAutoReplyWindow", configTypeBool, false); // v100: optional reply-hours window
    public static ConfigItem meeroAutoReplyWindowStart = addConfig("meeroAutoReplyWindowStart", configTypeInt, 23 * 60); // minutes of day
    public static ConfigItem meeroAutoReplyWindowEnd = addConfig("meeroAutoReplyWindowEnd", configTypeInt, 8 * 60); // minutes of day
    // --- v104: window pro - weekday bitmask (Sunday = bit 0 ... Saturday = bit 6, 127 = every day) ---
    public static ConfigItem meeroAutoReplyWindowDays = addConfig("meeroAutoReplyWindowDays", configTypeInt, 127);
    public static ConfigItem meeroAutoReplyNightTextOn = addConfig("meeroAutoReplyNightTextOn", configTypeBool, false); // v104: separate in-window reply text
    public static ConfigItem meeroAutoReplyNightText = addConfig("meeroAutoReplyNightText", configTypeString, "");
    // --- v105: keyword alert (JSON [{"id":0,"words":".."}] / id 0 = all chats) ---
    public static ConfigItem meeroKeywordAlert = addConfig("meeroKeywordAlert", configTypeBool, false);
    public static ConfigItem meeroKeywordRules = addConfig("meeroKeywordRules", configTypeString, "");
    // --- v184 (batch 2B): seed-sealed native stores for the automation group.
    // Opaque MAC-ed ciphertext blobs authored by libmeerocore; Java never
    // parses them. Legacy JSON keys above stay as the degraded fallback and
    // are imported+cleared on first native run. ---
    public static ConfigItem meeroAutoReplyStore = addConfig("meeroAutoReplyStore", configTypeString, "");
    public static ConfigItem meeroKeywordStore = addConfig("meeroKeywordStore", configTypeString, "");
    // --- v105: view-once guard (auto-save incoming once media to gallery) ---
    public static ConfigItem meeroOnceGuard = addConfig("meeroOnceGuard", configTypeBool, false);
    public static ConfigItem meeroOnceSavedCount = addConfig("meeroOnceSavedCount", configTypeInt, 0);
    // --- v109: once-guard legal/religious consent - granted ONCE and only
    // ever set by the explicit "موافق/gree" press; any declined entry keeps
    // it false so the consent sheet shows again on the next visit. ---
    public static ConfigItem meeroOnceConsent = addConfig("meeroOnceConsent", configTypeBool, false);
    // --- v106: per-chat lock (JSON [ids] / ids we muted = restore-on-unlock scope) ---
    public static ConfigItem meeroChatLock = addConfig("meeroChatLock", configTypeBool, false);
    public static ConfigItem meeroChatLockList = addConfig("meeroChatLockList", configTypeString, "");
    public static ConfigItem meeroChatLockMuted = addConfig("meeroChatLockMuted", configTypeString, "");
    // --- v107: chat lock pro - unlock method (0 = system biometric/device
    // lock, 1 = in-app 8-digit code). The code is kept as a salted SHA-256
    // hash ONLY on this device - if it is forgotten there is no recovery.
    // meeroChatsMenuFog = blurred fog behind the bottom-bar chats popup. ---
    public static ConfigItem meeroChatLockMethod = addConfig("meeroChatLockMethod", configTypeInt, 0);
    public static ConfigItem meeroChatLockCodeHash = addConfig("meeroChatLockCodeHash", configTypeString, "");
    public static ConfigItem meeroChatLockCodeSalt = addConfig("meeroChatLockCodeSalt", configTypeString, "");
    public static ConfigItem meeroChatsMenuFog = addConfig("meeroChatsMenuFog", configTypeBool, true);
    // --- MeeroX v110: auto-relock on app background (with grace choice) and
    // the local unlock-attempt audit log (JSON, newest first, capped). ---
    public static ConfigItem meeroChatLockAutoRelock = addConfig("meeroChatLockAutoRelock", configTypeBool, true);
    public static ConfigItem meeroChatLockRelockGrace = addConfig("meeroChatLockRelockGrace", configTypeInt, 0);
    public static ConfigItem meeroLockAuditLog = addConfig("meeroLockAuditLog", configTypeString, "");
    // --- MeeroX v111: watch message tracking (full person watch in shared
    // groups) + its optional instant alert. Tracking defaults OFF. ---
    public static ConfigItem meeroWatchMsgTrack = addConfig("meeroWatchMsgTrack", configTypeBool, false);
    public static ConfigItem meeroWatchMsgNotify = addConfig("meeroWatchMsgNotify", configTypeBool, true);

    // From NekoConfig
    public static ConfigItem useIPv6 = addConfig("IPv6", configTypeBool, false);
    public static ConfigItem hidePhone = addConfig("HidePhone", configTypeBool, true);
    public static ConfigItem ignoreBlocked = addConfig("IgnoreBlocked", configTypeBool, false);
    public static ConfigItem tabletMode = addConfig("TabletMode", configTypeInt, 0);

    public static ConfigItem typeface = addConfig("TypefaceUseDefault", configTypeBool, false);
    public static ConfigItem forceFontWeightFallback = addConfig("forceFontWeightFallback", configTypeBool, false);
    public static ConfigItem nameOrder = addConfig("NameOrder", configTypeInt, 1);
    public static ConfigItem mapPreviewProvider = addConfig("MapPreviewProvider", configTypeInt, 0);
    public static ConfigItem showAddToSavedMessages = addConfig("showAddToSavedMessages", configTypeBool, true);
    public static ConfigItem showReport = addConfig("showReport", configTypeBool, false);
    public static ConfigItem showViewHistory = addConfig("showViewHistory", configTypeBool, true);
    public static ConfigItem showAdminActions = addConfig("showAdminActions", configTypeBool, true);
    public static ConfigItem showChangePermissions = addConfig("showChangePermissions", configTypeBool, true);
    public static ConfigItem showDeleteDownloadedFile = addConfig("showDeleteDownloadedFile", configTypeBool, true);
    public static ConfigItem showMessageDetails = addConfig("showMessageDetails", configTypeBool, true);
    public static ConfigItem showTranslate = addConfig("showTranslate", configTypeBool, true);
    public static ConfigItem showRepeat = addConfig("showRepeat", configTypeBool, true);
    public static ConfigItem showShareMessages = addConfig("showShareMessages", configTypeBool, false);
    public static ConfigItem showMessageHide = addConfig("showMessageHide", configTypeBool, false);

    public static ConfigItem actionBarDecoration = addConfig("ActionBarDecoration", configTypeInt, 0);
    public static ConfigItem stickerSize = addConfig("stickerSize", configTypeFloat, 14.0f);
    public static ConfigItem unlimitedFavedStickers = addConfig("UnlimitedFavoredStickers", configTypeBool, false);
    public static ConfigItem unlimitedPinnedDialogs = addConfig("UnlimitedPinnedDialogs", configTypeBool, false);
    public static ConfigItem openArchiveOnPull = addConfig("OpenArchiveOnPull", configTypeBool, false);
    public static ConfigItem hideKeyboardOnChatScroll = addConfig("HideKeyboardOnChatScroll", configTypeBool, false);
    public static ConfigItem useSystemEmoji = addConfig("EmojiUseDefault", configTypeBool, false);
    public static ConfigItem rearVideoMessages = addConfig("RearVideoMessages", configTypeBool, false);
    public static ConfigItem hideAllTab = addConfig("HideAllTab", configTypeBool, false);

    public static ConfigItem sortByUnread = addConfig("sort_by_unread", configTypeBool, false);
    public static ConfigItem sortByUnmuted = addConfig("sort_by_unmuted", configTypeBool, true);
    public static ConfigItem sortByUser = addConfig("sort_by_user", configTypeBool, true);
    public static ConfigItem sortByContacts = addConfig("sort_by_contacts", configTypeBool, true);

    public static ConfigItem disableSystemAccount = addConfig("DisableSystemAccount", configTypeBool, false);
    public static ConfigItem skipOpenLinkConfirm = addConfig("SkipOpenLinkConfirm", configTypeBool, false);

    public static ConfigItem showIdAndDc = addConfig("ShowIdAndDc", configTypeBool, true);

    public static ConfigItem cachePath = addConfig("cache_path", configTypeString, "");
    public static ConfigItem customSavePath = addConfig("customSavePath", configTypeString, "MeeroX");

    public static ConfigItem translationProvider = addConfig("translationProvider", configTypeInt, 1);
    public static ConfigItem translateToLang = addConfig("TransToLang", configTypeString, ""); // "" -> translate to current language (MessageTrans.kt & Translator.kt)
    public static ConfigItem translateInputLang = addConfig("TransInputToLang", configTypeString, "en");
    public static ConfigItem googleCloudTranslateKey = addConfig("GoogleCloudTransKey", configTypeString, "");

    public static ConfigItem disableNotificationBubbles = addConfig("disableNotificationBubbles", configTypeBool, false);

    public static ConfigItem tabsTitleType = addConfig("TabTitleType", configTypeInt, NekoXConfig.TITLE_TYPE_TEXT);
    public static ConfigItem confirmAVMessage = addConfig("ConfirmAVMessage", configTypeBool, false);
    public static ConfigItem askBeforeCall = addConfig("AskBeforeCalling", configTypeBool, true);
    public static ConfigItem disableNumberRounding = addConfig("DisableNumberRounding", configTypeBool, false);

    public static ConfigItem dnsType = addConfig("DnsType", configTypeInt, DNS_TYPE_DEFAULT);
    public static ConfigItem customDoH = addConfig("CustomDoH", configTypeString, "");

    public static ConfigItem mediaPreview = addConfig("MediaPreview", configTypeBool, true);

    public static ConfigItem disableVibration = addConfig("DisableVibration", configTypeBool, false);
    public static ConfigItem autoPauseVideo = addConfig("AutoPauseVideo", configTypeBool, false);
    public static ConfigItem disableProximityEvents = addConfig("DisableProximityEvents", configTypeBool, false);

    public static ConfigItem ignoreContentRestrictions = addConfig("ignoreContentRestrictions", configTypeBool, true);
    public static ConfigItem useChatAttachMediaMenu = addConfig("UseChatAttachEnterMenu", configTypeBool, true);
    public static ConfigItem disableLinkPreviewByDefault = addConfig("DisableLinkPreviewByDefault", configTypeBool, false);
    public static ConfigItem sendCommentAfterForward = addConfig("SendCommentAfterForward", configTypeBool, true);
    public static ConfigItem disableTrending = addConfig("DisableTrending", configTypeBool, true);
    public static ConfigItem dontSendGreetingSticker = addConfig("DontSendGreetingSticker", configTypeBool, true);
    public static ConfigItem hideTimeForSticker = addConfig("HideTimeForSticker", configTypeBool, false);
    public static ConfigItem takeGIFasVideo = addConfig("TakeGIFasVideo", configTypeBool, false);
    public static ConfigItem maxRecentStickerCount = addConfig("maxRecentStickerCount", configTypeInt, 20);
    public static ConfigItem disableSwipeToNext = addConfig("disableSwipeToNextChannel", configTypeBool, false);
    public static ConfigItem disableSwipeToNextTopic = addConfig("disableSwipeToNextTopic", configTypeBool, false);
    public static ConfigItem disableChoosingSticker = addConfig("disableChoosingSticker", configTypeBool, false);
    public static ConfigItem hideGroupSticker = addConfig("hideGroupSticker", configTypeBool, false);
    public static ConfigItem rememberAllBackMessages = addConfig("rememberAllBackMessages", configTypeBool, false);
    public static ConfigItem hideSendAsChannel = addConfig("hideSendAsChannel", configTypeBool, false);
    public static ConfigItem showSpoilersDirectly = addConfig("showSpoilersDirectly", configTypeBool, false);

    public static ConfigItem disableAutoDownloadingWin32Executable = addConfig("Win32ExecutableFiles", configTypeBool, true);
    public static ConfigItem disableAutoDownloadingArchive = addConfig("ArchiveFiles", configTypeBool, true);

    public static ConfigItem customAudioBitrate = addConfig("customAudioBitrate", configTypeInt, 32);
    public static ConfigItem enhancedFileLoader = addConfig("enhancedFileLoader", configTypeBool, false);
    public static ConfigItem uploadBoost = addConfig("uploadBoost", configTypeBool, false);
    public static ConfigItem useOSMDroidMap = addConfig("useOSMDroidMap", configTypeBool, false);
    public static ConfigItem mapDriftingFixForGoogleMaps = addConfig("mapDriftingFixForGoogleMaps", configTypeBool, true);

    public static ConfigItem localPremium = addConfig("localPremium", configTypeBool, false);

    public static ConfigItem usePersianCalendar = addConfig("UsePersianCalendar", configTypeBool, false);
    public static ConfigItem displayPersianCalendarByLatin = addConfig("DisplayPersianCalendarByLatin", configTypeBool, false);

    public static ConfigItem minimizedStickerCreator = addConfig("minimizedStickerCreator", configTypeBool, false);

    // --- Ghost Mode ---
    public static ConfigItem sendReadMessagePackets = addConfig("sendReadMessagePackets", configTypeBool, true);
    public static ConfigItem sendReadStoriesPackets = addConfig("sendReadStoriesPackets", configTypeBool, true);
    public static ConfigItem sendOnlinePackets = addConfig("sendOnlinePackets", configTypeBool, true);
    public static ConfigItem sendUploadProgress = addConfig("sendUploadProgress", configTypeBool, true);
    public static ConfigItem sendOfflinePacketAfterOnline = addConfig("sendOfflinePacketAfterOnline", configTypeBool, false);
    public static ConfigItem markReadAfterSend = addConfig("markReadAfterSend", configTypeBool, true);
    public static ConfigItem showGhostInDrawer = addConfig("showGhostInDrawer", configTypeBool, false);
    public static ConfigItem showGhostModeStatus = addConfig("showGhostModeStatus", configTypeBool, false);

    // --- Locked Status ---
    public static ConfigItem sendReadMessagePacketsLocked = addConfig("sendReadMessagePacketsLocked", configTypeBool, false);
    public static ConfigItem sendReadStoriesPacketsLocked = addConfig("sendReadStoriesPacketsLocked", configTypeBool, false);
    public static ConfigItem sendOnlinePacketsLocked = addConfig("sendOnlinePacketsLocked", configTypeBool, false);
    public static ConfigItem sendUploadProgressLocked = addConfig("sendUploadProgressLocked", configTypeBool, false);
    public static ConfigItem sendOfflinePacketAfterOnlineLocked = addConfig("sendOfflinePacketAfterOnlineLocked", configTypeBool, false);
    // --- Ghost Mode ---

    static {
        init();
    }

    public static void init() {
        loadConfig(false);
    }

    public static ConfigItem addConfig(String k, int t, Object d) {
        ConfigItem a = new ConfigItem(k, t, d);
        configs.add(a);
        return a;
    }

    public static void loadConfig(boolean force) {
        synchronized (sync) {
            if (configLoaded && !force) {
                return;
            }
            if (ApplicationLoader.applicationContext == null) {
                return;
            }
            for (int i = 0; i < configs.size(); i++) {
                ConfigItem o = configs.get(i);

                try {
                    if (o.type == configTypeBool) {
                        o.value = getPreferences().getBoolean(o.key, (boolean) o.defaultValue);
                    }
                    if (o.type == configTypeInt) {
                        o.value = getPreferences().getInt(o.key, (int) o.defaultValue);
                    }
                    if (o.type == configTypeLong) {
                        o.value = getPreferences().getLong(o.key, (Long) o.defaultValue);
                    }
                    if (o.type == configTypeFloat) {
                        o.value = getPreferences().getFloat(o.key, (Float) o.defaultValue);
                    }
                    if (o.type == configTypeString) {
                        o.value = getPreferences().getString(o.key, (String) o.defaultValue);
                    }
                    if (o.type == configTypeSetInt) {
                        Set<String> ss = getPreferences().getStringSet(o.key, new HashSet<>());
                        HashSet<Integer> si = new HashSet<>();
                        for (String s : ss) {
                            si.add(Integer.parseInt(s));
                        }
                        o.value = si;
                    }
                    if (o.type == configTypeMapIntInt) {
                        String cv = getPreferences().getString(o.key, "");
                        if (cv.isEmpty()) {
                            o.value = new HashMap<Integer, Integer>();
                        } else {
                            try {
                                byte[] data = Base64.decode(cv, Base64.DEFAULT);
                                ObjectInputStream ois = new ObjectInputStream(
                                        new ByteArrayInputStream(data));
                                o.value = ois.readObject();
                                if (o.value == null) {
                                    o.value = new HashMap<Integer, Integer>();
                                }
                                ois.close();
                            } catch (Exception e) {
                                o.value = new HashMap<Integer, Integer>();
                            }
                        }
                    }
                } catch (ClassCastException | NumberFormatException e) {
                    FileLog.e("Invalid config value for " + o.key, e);
                    o.value = o.defaultValue;
                    getPreferences().edit().remove(o.key).apply();
                }
            }
            // MeeroX v122 one-time migration: the old meeroIosBubbles switch
            // became the meeroBubbleStyle picker (0=stock, 1=official iOS,
            // 4=classic, 5=sharp, 6=instagram, 7=whatsapp). First launch after the
            // update maps it: off -> 0, on or never touched -> 1, so the
            // update never flips anyone's screen on or off by itself.
            if (!getPreferences().contains("meeroBubbleStyle")) {
                int meeroMigrated = getPreferences().getBoolean("meeroIosBubbles", true) ? 1 : 0;
                getPreferences().edit().putInt("meeroBubbleStyle", meeroMigrated).apply();
                meeroBubbleStyle.value = meeroMigrated;
            }
            if (!configLoaded)
                getPreferences().registerOnSharedPreferenceChangeListener(CloudSettingsHelper.listener);
            for (int a = 1; a <= 5; a++) {
                datacenterInfos.add(new DatacenterInfo(a));
            }
            configLoaded = true;
        }
    }

    public static class DatacenterInfo {

        public int id;

        public long pingId;
        public long ping;
        public boolean checking;
        public boolean available;
        public long availableCheckTime;

        public DatacenterInfo(int i) {
            id = i;
        }
    }

    public static boolean fixDriftingForGoogleMaps() {
        return !useOSMDroidMap.Bool() && mapDriftingFixForGoogleMaps.Bool();
    }

    // --- Ghost Mode ---
    public static boolean isGhostModeActive() {
        for (Pair<ConfigItem, ConfigItem> pair : ghostToggleItems) {
            ConfigItem item = pair.first;
            ConfigItem lockedItem = pair.second;
            if (!lockedItem.Bool()) {
                boolean currentValue = item.Bool();
                boolean isGhostState = (item == sendOfflinePacketAfterOnline) == currentValue;

                if (!isGhostState) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void setGhostMode(boolean enabled) {
        for (Pair<ConfigItem, ConfigItem> pair : ghostToggleItems) {
            ConfigItem item = pair.first;
            ConfigItem lockedItem = pair.second;
            if (!lockedItem.Bool()) {
                boolean targetValue = (item == sendOfflinePacketAfterOnline) == enabled;
                item.setConfigBool(targetValue);
            }
        }
    }

    public static void toggleGhostMode() {
        boolean newState = !isGhostModeActive();
        setGhostMode(newState);

        boolean sendOnlineNow = !newState && !sendOfflinePacketAfterOnlineLocked.Bool() && sendOfflinePacketAfterOnline.Bool();
        AyuGhostUtils.performStatusRequest(sendOnlineNow);
    }

    private static final List<Pair<ConfigItem, ConfigItem>> ghostToggleItems = Arrays.asList(
            new Pair<>(sendReadMessagePackets, sendReadMessagePacketsLocked),
            new Pair<>(sendReadStoriesPackets, sendReadStoriesPacketsLocked),
            new Pair<>(sendOnlinePackets, sendOnlinePacketsLocked),
            new Pair<>(sendUploadProgress, sendUploadProgressLocked),
            new Pair<>(sendOfflinePacketAfterOnline, sendOfflinePacketAfterOnlineLocked)
    );
    // --- Ghost Mode ---

    public static Map<String, Integer> getConfigTypes() {
        synchronized (sync) {
            Map<String, Integer> types = new HashMap<>();
            for (ConfigItem o : configs) {
                types.put(o.getKey(), o.type);
            }
            return types;
        }
    }
}
