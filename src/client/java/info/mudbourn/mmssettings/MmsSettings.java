package info.mudbourn.mmssettings;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MmsSettings {
    public static final String MOD_ID = "mms_settings";
    public static final String OPAC_ID = "openpartiesandclaims";
    public static final String MODMENU_ID = "modmenu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private MmsSettings() {
    }

    public static boolean opacLoaded() {
        return FabricLoader.getInstance().isModLoaded(OPAC_ID);
    }

    public static boolean modMenuLoaded() {
        return FabricLoader.getInstance().isModLoaded(MODMENU_ID);
    }
}
