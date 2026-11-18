package info.mudbourn.mmssettings.hub;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import info.mudbourn.mmssettings.MmsSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.Screen;

final class ModMenuSources {
    record Entry(String id, String name, Function<Screen, Screen> factory) {
    }

    private ModMenuSources() {
    }

    static List<Entry> collect() {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (EntrypointContainer<ModMenuApi> container : FabricLoader.getInstance().getEntrypointContainers(MmsSettings.MODMENU_ID, ModMenuApi.class)) {
            String owner = container.getProvider().getMetadata().getId();

            try {
                ModMenuApi api = container.getEntrypoint();
                add(entries, seen, owner, api.getModConfigScreenFactory());

                for (Map.Entry<String, ConfigScreenFactory<?>> provided : api.getProvidedConfigScreenFactories().entrySet()) {
                    add(entries, seen, provided.getKey(), provided.getValue());
                }
            } catch (Throwable error) {
                MmsSettings.LOGGER.warn("Skipping Mod Menu entrypoint of {}", owner, error);
            }
        }

        entries.sort(Comparator.comparing(entry -> entry.name().toLowerCase()));

        return entries;
    }

    static Optional<Function<Screen, Screen>> factoryFor(String modId) {
        return collect().stream()
            .filter(entry -> entry.id().equals(modId))
            .map(Entry::factory)
            .findFirst();
    }

    private static void add(List<Entry> entries, Set<String> seen, String modId, ConfigScreenFactory<?> factory) {
        if (factory == null || MmsSettings.MOD_ID.equals(modId) || !seen.add(modId)) {
            return;
        }

        String name = FabricLoader.getInstance()
            .getModContainer(modId)
            .map(ModContainer::getMetadata)
            .map(metadata -> metadata.getName())
            .orElse(modId);

        entries.add(new Entry(modId, name, factory::create));
    }
}
