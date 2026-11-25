package info.mudbourn.mmssettings.hub;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
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

        if (FabricLoader.getInstance().isModLoaded("opacui")) {
            tiles.add(new Tile(label("opacui"),
                parent -> construct("mervyn.opacui.client.gui.PartyScreen", parent), true));
        }

        if (FabricLoader.getInstance().isModLoaded("origins-legacy")) {
            tiles.add(new Tile(label("origin"),
                parent -> construct("io.github.apace100.origins.screen.ViewOriginScreen", null), true));
        }

        mod("voicechat", "voicechat", "de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen", tiles);
        mod("xaerominimap", "xaerominimap", null, tiles);

        if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            tiles.add(new Tile(label("waypoints"), Targets::xaeroWaypoints, true));
        }

        mod("bettercombat", "bettercombat", null, tiles);
        mod("iris", "iris", "net.irisshaders.iris.gui.screen.ShaderPackScreen", tiles);
        mod("distanthorizons", "distanthorizons", null, tiles);
        if (FabricLoader.getInstance().isModLoaded("mms_economy")) {
            tiles.add(new Tile(label("mms_economy"), Targets::economyHub, true));
        }

        if (FabricLoader.getInstance().isModLoaded("mms_dungeons")) {
            tiles.add(new Tile(label("adventurer"), AdventurerToolsScreen::new, true));
        }

        if (FabricLoader.getInstance().isModLoaded("mms_combat")) {
            tiles.add(new Tile(label("loadout"), Targets::loadout, true));
        }

        if (FabricLoader.getInstance().isModLoaded("mms_weapons")) {
            tiles.add(new Tile(label("spell_binds"),
                parent -> construct("info.mudbourn.mmsweapons.client.spellengine.SpellBindsScreen", parent), true));
        }

        if (FabricLoader.getInstance().isModLoaded("essential")) {
            tiles.add(new Tile(label("emotes"), Targets::essentialEmotes, false));
        }

        tiles.add(new Tile(label("controls"), parent -> new KeyBindsScreen(parent, Minecraft.getInstance().options), false));
        tiles.add(new Tile(label("sounds"), parent -> new SoundOptionsScreen(parent, Minecraft.getInstance().options), false));
        tiles.add(new Tile(label("video"), parent -> new VideoSettingsScreen(parent, Minecraft.getInstance(), Minecraft.getInstance().options), false));

        return tiles;
    }

    static List<Tile> adventurerTools() {
        return List.of(
            new Tile(
                label("my_bounties"),
                parent -> construct("info.mudbourn.mmsdungeons.client.guild.MyBountiesScreen", parent),
                true
            ),
            new Tile(label("guild_info"), parent -> command("guild info"), true),
            new Tile(label("guild_list"), parent -> command("guild list"), true)
        );
    }

    // Runs a command whose reply lands in chat, and closes this screen.
    private static Screen command(String command) {
        Minecraft.getInstance().player.connection.sendCommand(command);
        return null;
    }

    // Asks the server for economy's hub and closes this one.
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

    // Runs /loadout so the server opens the loadout screen, and closes this one.
    private static Screen loadout(Screen parent) {
        Minecraft.getInstance().player.connection.sendCommand("loadout");
        return null;
    }

    // Asks Essential to open its wardrobe on the emote wheel editor, and closes this one.
    private static Screen essentialEmotes(Screen parent) {
        try {
            Class<?> wardrobe = Class.forName("gg.essential.gui.wardrobe.Wardrobe");
            Class<?> category = Class.forName("gg.essential.gui.wardrobe.WardrobeCategory");
            Object emotes = Class.forName("gg.essential.gui.wardrobe.WardrobeCategory$Emotes").getField("INSTANCE").get(null);
            Constructor<?> constructor = wardrobe.getConstructor(category, boolean.class);
            Class<?> function0 = Class.forName("kotlin.jvm.functions.Function0");
            Object factory = Proxy.newProxyInstance(
                function0.getClassLoader(),
                new Class<?>[] {function0},
                (proxy, method, args) -> switch (method.getName()) {
                    case "invoke" -> constructor.newInstance(emotes, true);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> "EssentialEmotesFactory";
                }
            );

            Class.forName("gg.essential.util.GuiUtil")
                .getMethod("openScreen", Class.class, function0)
                .invoke(null, wardrobe, factory);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot open Essential's emote editor", error);
        }
        return null;
    }

    // Opens a screen by class name. A null parent means the screen takes no arguments.
    private static Screen construct(String className, Screen parent) {
        try {
            Class<?> type = Class.forName(className);
            Object screen = parent == null
                ? type.getDeclaredConstructor().newInstance()
                : type.getDeclaredConstructor(Screen.class).newInstance(parent);
            return (Screen) screen;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not open " + className, error);
        }
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

        if (modId.equals("distanthorizons") && factory.isEmpty()) {
            factory = Optional.of(Targets::distantHorizonsSettings);
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

    private static Screen distantHorizonsSettings(Screen parent) {
        try {
            Class<?> type = Class.forName("com.seibel.distanthorizons.common.wrappers.gui.GetConfigScreen");

            return (Screen) type.getMethod("getScreen", Screen.class).invoke(null, parent);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot open Distant Horizons settings", error);
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
