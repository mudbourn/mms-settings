package info.mudbourn.mmssettings.opac;

import info.mudbourn.mmssettings.MmsSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import xaero.pac.client.api.OpenPACClientAPI;

public final class OpacScreens {
    private OpacScreens() {
    }

    public static Screen create(Screen parent) {
        try {
            return new OpacScreen(parent);
        } catch (Throwable error) {
            MmsSettings.LOGGER.warn("MMS OPAC view failed, opening the OPAC menu instead", error);
            OpenPACClientAPI.get().openMainMenuScreen(parent, parent);

            return Minecraft.getInstance().screen != null ? Minecraft.getInstance().screen : parent;
        }
    }
}
