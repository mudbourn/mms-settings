package info.mudbourn.mmssettings.hub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import info.mudbourn.mmssettings.MmsSettings;
import net.fabricmc.loader.api.FabricLoader;

final class ConfigFiles {
    record Entry(String relative, Path path) {
    }

    private static final Set<String> EXTENSIONS = Set.of("json", "json5", "toml", "properties", "cfg", "conf", "ini", "yaml", "yml", "txt", "snbt");
    private static final int MAX_DEPTH = 3;

    private ConfigFiles() {
    }

    static List<Entry> collect() {
        Path root = FabricLoader.getInstance().getConfigDir();
        List<Entry> entries = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(root, MAX_DEPTH)) {
            walk.filter(Files::isRegularFile)
                .filter(ConfigFiles::isConfigFile)
                .forEach(path -> entries.add(new Entry(root.relativize(path).toString().replace('\\', '/'), path)));
        } catch (IOException error) {
            MmsSettings.LOGGER.warn("Could not list the config directory", error);
        }

        entries.sort(Comparator.comparing(entry -> entry.relative().toLowerCase()));

        return entries;
    }

    private static boolean isConfigFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');

        return dot > 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }
}
