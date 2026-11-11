package info.mudbourn.mmssettings.hub;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import info.mudbourn.mmssettings.MmsSettings;
import info.mudbourn.mmssettings.opac.OpacScreens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

final class Targets {
    record Tile(Component label, Function<Screen, Screen> factory, boolean needsWorld) {
    }

    private Targets() {
    }

    static List<Tile> resolve() {
        List<Tile> tiles = new ArrayList<>();

        if (MmsSettings.opacLoaded()) {
            tiles.add(new Tile(label("opac"), OpacScreens::create, false));
        }

        mod("voicechat", "voicechat", "de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen", tiles);
        mod("xaerominimap", "xaerominimap", null, tiles);

        if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            tiles.add(new Tile(label("waypoints"), Targets::xaeroWaypoints, true));
        }

        mod("bettercombat", "bettercombat", null, tiles);
        if (FabricLoader.getInstance().isModLoaded("mms_economy")) {
            tiles.add(new Tile(label("mms_economy"), Targets::economyHub, true));
        }

        tiles.add(new Tile(label("controls"), parent -> new KeyBindsScreen(parent, Minecraft.getInstance().options), false));
        tiles.add(new Tile(label("sounds"), parent -> new SoundOptionsScreen(parent, Minecraft.getInstance().options), false));
        tiles.add(new Tile(label("video"), parent -> new VideoSettingsScreen(parent, Minecraft.getInstance(), Minecraft.getInstance().options), false));

        return tiles;
    }

    // Economy's hub is opened by the server: ask for it the same way its own key does.
    // The hub closes, and economy's OpenHub receiver puts its screen up.
    private static Screen economyHub(Screen parent) {
        try {
            Object request = Class.forName("info.mudbourn.mmseconomy.network.EconomyNetworking$RequestHub")
                .getDeclaredConstructor().newInstance();
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                (net.minecraft.network.protocol.common.custom.CustomPacketPayload) request);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("MMS Economy hub request not found", error);
        }
        return null;
    }

    private static Component label(String key) {
        return Component.translatable("mms_settings.hub.tile." + key);
    }

    private static void mod(String modId, String labelKey, String screenClass, List<Tile> tiles) {
        if (!FabricLoader.getInstance().isModLoaded(modId)) {
            return;
        }

        Optional<Function<Screen, Screen>> factory = MmsSettings.modMenuLoaded()
            ? ModMenuSources.factoryFor(modId)
            : Optional.empty();

        if (factory.isEmpty() && screenClass != null) {
            factory = Optional.of(parent -> newScreen(screenClass, parent));
        }

        if (modId.equals("xaerominimap") && factory.isEmpty()) {
            factory = Optional.of(Targets::xaeroSettings);
        }

        factory.ifPresent(found -> tiles.add(new Tile(label(labelKey), found, false)));
    }

    private static Screen newScreen(String className, Screen parent) {
        try {
            Constructor<?> constructor = Class.forName(className).getConstructor(Screen.class);

            return (Screen) constructor.newInstance(parent);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot open " + className, error);
        }
    }

    private static Screen xaeroSettings(Screen parent) {
        try {
            Object mod = Class.forName("xaero.common.HudMod").getField("INSTANCE").get(null);
            Object helper = mod.getClass().getMethod("getGuiHelper").invoke(mod);

            return (Screen) helper.getClass().getMethod("getMainSettingsScreen", Screen.class).invoke(helper, parent);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot open Xaero's Minimap settings", error);
        }
    }

    private static Screen xaeroWaypoints(Screen parent) {
        try {
            Object mod = Class.forName("xaero.common.HudMod").getField("INSTANCE").get(null);
            Object module = Class.forName("xaero.hud.minimap.BuiltInHudModules").getField("MINIMAP").get(null);
            Object session = module.getClass().getMethod("getCurrentSession").invoke(module);

            for (Constructor<?> constructor : Class.forName("xaero.common.gui.GuiWaypoints").getConstructors()) {
                if (constructor.getParameterCount() == 4) {
                    return (Screen) constructor.newInstance(mod, session, parent, parent);
                }
            }

            throw new IllegalStateException("No GuiWaypoints constructor with four parameters");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot open Xaero's waypoints menu", error);
        }
    }
}
