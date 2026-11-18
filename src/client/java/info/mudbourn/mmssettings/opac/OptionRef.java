package info.mudbourn.mmssettings.opac;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageAPI;
import xaero.pac.client.player.config.api.v2.IPlayerConfigStringableOptionClientStorageAPI;

final class OptionRef {
    enum Kind {
        BOOLEAN,
        CYCLE,
        TEXT
    }

    final IPlayerConfigClientStorageAPI storage;
    final IPlayerConfigStringableOptionClientStorageAPI<Object> option;
    final String id;
    final List<String> path;
    final Component name;
    final Component comment;
    final Kind kind;

    @SuppressWarnings("unchecked")
    OptionRef(IPlayerConfigClientStorageAPI storage, IPlayerConfigStringableOptionClientStorageAPI<?> option) {
        this.storage = storage;
        this.option = (IPlayerConfigStringableOptionClientStorageAPI<Object>) option;
        this.id = option.getId();
        this.path = Arrays.asList(id.replaceFirst("^playerConfig\\.", "").split("\\."));
        this.name = Component.translatable(option.getTranslation(), option.getTranslationArgs());
        this.comment = commentOf(option);
        this.kind = kindOf(option);
    }

    String lastSegment() {
        return path.get(path.size() - 1);
    }

    String section() {
        return String.join(".", path.subList(0, path.size() - 1));
    }

    boolean matches(String needle) {
        return needle.isEmpty()
            || name.getString().toLowerCase(Locale.ROOT).contains(needle)
            || id.toLowerCase(Locale.ROOT).contains(needle);
    }

    @SuppressWarnings("unchecked")
    List<Object> cycleValues() {
        try {
            Object spec = option.getOption();
            Object getter = spec.getClass().getMethod("getClientSideListGetter").invoke(spec);
            Object values = ((Function<Object, Object>) getter).apply(storage);

            return values instanceof List<?> list ? List.copyOf(list) : List.of();
        } catch (Throwable error) {
            return List.of();
        }
    }

    Component display(Object value) {
        if (value == null) {
            return Component.translatable("mms_settings.opac.default");
        }

        try {
            return option.getComponentWriterCast().apply(value);
        } catch (Throwable error) {
            return Component.literal(String.valueOf(value));
        }
    }

    @SuppressWarnings("unchecked")
    String text(Object value) {
        try {
            Method writer = option.getClass().getMethod("getStringWriterCast");

            return ((Function<Object, String>) writer.invoke(option)).apply(value);
        } catch (Throwable error) {
            return String.valueOf(value);
        }
    }

    private static Kind kindOf(IPlayerConfigStringableOptionClientStorageAPI<?> option) {
        if (option.getType() == Boolean.class) {
            return Kind.BOOLEAN;
        }

        return hasListGetter(option) ? Kind.CYCLE : Kind.TEXT;
    }

    private static boolean hasListGetter(IPlayerConfigStringableOptionClientStorageAPI<?> option) {
        for (Method method : option.getOption().getClass().getMethods()) {
            if (method.getName().equals("getClientSideListGetter")) {
                return true;
            }
        }

        return false;
    }

    private static Component commentOf(IPlayerConfigStringableOptionClientStorageAPI<?> option) {
        String key = option.getCommentTranslation();

        if (key != null && Language.getInstance().has(key)) {
            return Component.translatable(key, option.getCommentTranslationArgs());
        }

        String plain = option.getComment();

        return plain == null ? Component.empty() : Component.literal(plain);
    }
}
