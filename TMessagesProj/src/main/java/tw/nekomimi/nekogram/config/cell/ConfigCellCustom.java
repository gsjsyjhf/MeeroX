package tw.nekomimi.nekogram.config.cell;

import androidx.recyclerview.widget.RecyclerView;

public class ConfigCellCustom extends AbstractConfigCell implements WithKey {
    public static final int CUSTOM_ITEM_StickerSize = 998;
    public static final int CUSTOM_ITEM_CharBlurAlpha = 997;
    public static final int CUSTOM_ITEM_EmojiSet = 996;
    public static final int CUSTOM_ITEM_Temperature = 995;
    // MeeroX v219: inline deleted-trash color strip (NekoExperimental screen)
    public static final int CUSTOM_ITEM_MeeroTrashColors = 994;
    // MeeroX v256: live chat-top-strip preview (MeeroSettings screen,
    // collapsible "شريط الدردشة العلوي" section)
    public static final int CUSTOM_ITEM_MeeroHeaderPreview = 993;

    public final int type;
    public boolean enabled;
    private final String key;

    public ConfigCellCustom(String key, int type, boolean enabled) {
        this.key = key;
        this.type = type;
        this.enabled = enabled;
    }

    public int getType() {
        return type;
    }

    public String getKey() {
        return this.key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder holder) {
        // Not Used
    }
}
