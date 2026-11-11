package info.mudbourn.mmssettings;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmssettings.hub.HubScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MmsSettingsClient implements ClientModInitializer {
    private static KeyMapping openHub;

    @Override
    public void onInitializeClient() {
        openHub = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.mms_settings.open_hub",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MmsSettings.MOD_ID, "main"))
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openHub.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new HubScreen(null));
                }
            }
        });
    }
}
