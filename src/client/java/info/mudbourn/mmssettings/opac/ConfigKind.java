package info.mudbourn.mmssettings.opac;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageAPI;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageManagerAPI;

enum ConfigKind {
    MY("my"),
    PARTY("party"),
    DEFAULT("default"),
    SERVER("server"),
    WILDERNESS("wilderness"),
    EXPIRED("expired");

    private final String key;

    ConfigKind(String key) {
        this.key = key;
    }

    Component label() {
        return Component.translatable("mms_settings.opac.config." + key);
    }

    ConfigKind next() {
        return values()[(ordinal() + 1) % values().length];
    }

    IPlayerConfigClientStorageAPI storage(IPlayerConfigClientStorageManagerAPI manager) {
        return switch (this) {
            case MY -> manager.getMyPlayerConfig();
            case PARTY -> manager.getPartyClaimsConfig();
            case DEFAULT -> manager.getDefaultPlayerConfig();
            case SERVER -> manager.getServerClaimsConfig();
            case WILDERNESS -> manager.getWildernessConfig();
            case EXPIRED -> manager.getExpiredClaimsConfig();
        };
    }

    void openOriginal(IPlayerConfigClientStorageManagerAPI manager, Screen parent, Screen escape) {
        switch (this) {
            case MY -> manager.openMyPlayerConfigScreen(parent, escape);
            case PARTY -> manager.openPartyClaimsConfigScreen(parent, escape);
            case DEFAULT -> manager.openDefaultPlayerConfigScreen(parent, escape);
            case SERVER -> manager.openServerClaimsConfigScreen(parent, escape);
            case WILDERNESS -> manager.openWildernessConfigScreen(parent, escape);
            case EXPIRED -> manager.openExpiredClaimsConfigScreen(parent, escape);
        }
    }
}
