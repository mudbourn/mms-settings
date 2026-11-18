package info.mudbourn.mmssettings;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmssettings.hub.HubScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class MmsSettingsClient implements ClientModInitializer {
    private static KeyMapping openHub;

    public static KeyMapping openHubKey() {
        return openHub;
    }

    @Override
    public void onInitializeClient() {
        openHub = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.mms_settings.open_hub",
                InputConstants.KEY_SEMICOLON,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MmsSettings.MOD_ID, "main"))
            )
        );

        Speedometer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openHub.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new HubScreen(null));
                }
            }
        });
    }
}
