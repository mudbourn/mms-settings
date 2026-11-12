package info.mudbourn.mmssettings.opac;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.mudbourn.mmssettings.ui.RowList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageAPI;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageManagerAPI;

class OpacScreen extends Screen {
    private static final Pattern FAMILY = Pattern.compile("^(.+?)(?:By)?(Players|Mobs|Other)$");
    private static final Pattern CATEGORY_WORDS = Pattern.compile("(?i)\\s*\\b(by\\s+)?(players?|mobs?|other)\\b");
    private static final int ROW_HEIGHT = 24;
    private static final int VALUE_WIDTH = 110;

    private enum Column {
        PLAYERS("players"),
        MOBS("mobs"),
        OTHER("other");

        final String key;

        Column(String key) {
            this.key = key;
        }

        Component label() {
            return Component.translatable("mms_settings.opac.col." + key);
        }
    }

    private final Screen parent;
    private final IPlayerConfigClientStorageManagerAPI manager = OpenPACClientAPI.get().getPlayerConfigClientStorageManager();
    private ConfigKind kind = ConfigKind.MY;
    private String subConfig;
    private String search = "";
    private List<String> folder = List.of();
    private boolean byCategory;
    private Column selected = Column.PLAYERS;
    private RowList list;
    private boolean writeFailed;

    OpacScreen(Screen parent) {
        super(Component.translatable("mms_settings.opac.title"));
        this.parent = parent;
        manager.getMyPlayerConfig();
    }

    @Override
    protected void init() {
        int left = width / 2 - RowList.ROW_WIDTH / 2;
        int third = (RowList.ROW_WIDTH - 8) / 3;

        addRenderableWidget(
            Button.builder(kind.label(), button -> {
                kind = kind.next();
                subConfig = null;
                folder = List.of();
                byCategory = false;
                rebuildWidgets();
            })
                .bounds(left, 22, third, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(subLabel(), button -> {
                cycleSubConfig();
                rebuildWidgets();
            })
                .bounds(left + third + 4, 22, third, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable("mms_settings.opac.original"),
                button -> kind.openOriginal(manager, this, parent)
            )
                .bounds(left + (third + 4) * 2, 22, third, 20)
                .build()
        );

        EditBox searchBox = new EditBox(font, left, 46, RowList.ROW_WIDTH, 20, Component.translatable("mms_settings.search"));
        searchBox.setHint(Component.translatable("mms_settings.search"));
        searchBox.setValue(search);
        searchBox.setResponder(text -> {
            search = text;
            refill();
        });
        addRenderableWidget(searchBox);

        list = addRenderableWidget(new RowList(minecraft, width, height - 100, 72, ROW_HEIGHT));

        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 154, height - 26, 150, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable("mms_settings.opac.menu"),
                button -> OpenPACClientAPI.get().openMainMenuScreen(this, parent)
            )
                .bounds(width / 2 + 4, height - 26, 150, 20)
                .build()
        );

        refill();
        setInitialFocus(searchBox);
    }

    private IPlayerConfigClientStorageAPI activeStorage() {
        IPlayerConfigClientStorageAPI main = kind.storage(manager);

        if (main != null && subConfig != null && main.subConfigExists(subConfig)) {
            return main.getSubConfig(subConfig);
        }

        return main;
    }

    private Component subLabel() {
        return subConfig == null
            ? Component.translatable("mms_settings.opac.sub_main")
            : Component.translatable("mms_settings.opac.sub", subConfig);
    }

    private void cycleSubConfig() {
        IPlayerConfigClientStorageAPI main = kind.storage(manager);
        List<String> ids = main == null ? List.of() : main.getSubConfigIds();

        if (ids.isEmpty()) {
            subConfig = null;
        } else if (subConfig == null) {
            subConfig = ids.get(0);
        } else {
            int next = ids.indexOf(subConfig) + 1;
            subConfig = next >= ids.size() ? null : ids.get(next);
        }
    }

    private void onWriteFailed() {
        writeFailed = true;
    }

    private void open(List<String> path) {
        folder = path;
        byCategory = false;
        refill();
    }

    private List<OptionRef> visibleOptions() {
        IPlayerConfigClientStorageAPI storage = activeStorage();
        String needle = search.toLowerCase(Locale.ROOT).trim();
        List<OptionRef> refs = new ArrayList<>();

        if (storage == null) {
            return refs;
        }

        storage.options().forEach(option -> {
            var spec = option.getOption();

            if (spec.isDirectlyConfigurable() && spec.getConfigTypeFilter().test(storage.getType())) {
                OptionRef ref = new OptionRef(storage, option);

                if (ref.matches(needle)) {
                    refs.add(ref);
                }
            }
        });

        return refs;
    }

    private void refill() {
        list.clearRows();
        List<OptionRef> refs = visibleOptions();

        if (refs.isEmpty()) {
            list.addRow(new MessageRow(Component.translatable("mms_settings.opac.empty")));

            return;
        }

        if (!search.isBlank()) {
            refs.sort(Comparator.comparing(ref -> ref.name.getString().toLowerCase(Locale.ROOT)));
            refs.forEach(ref -> list.addRow(new OptionRow(ref)));

            return;
        }

        List<OptionRef> here = refs.stream()
            .filter(ref -> ref.path.subList(0, ref.path.size() - 1).equals(folder))
            .toList();

        Map<String, EnumMap<Column, OptionRef>> families = familiesOf(here);

        if (!folder.isEmpty() || byCategory) {
            list.addRow(new BackRow());
        }

        if (byCategory) {
            list.addRow(new SwitchRow());
            families.values().stream()
                .filter(cells -> cells.containsKey(selected))
                .forEach(cells -> list.addRow(new OptionRow(cells.get(selected), familyLabel(cells))));

            return;
        }

        for (OptionRef ref : here) {
            if (families.values().stream().noneMatch(cells -> cells.containsValue(ref))) {
                list.addRow(new OptionRow(ref));
            }
        }

        if (!families.isEmpty()) {
            Component text = Component.translatable("mms_settings.opac.by_category", families.size());
            list.addRow(new FolderRow(text, () -> {
                byCategory = true;
                refill();
            }));
        }

        childFolders(refs).forEach((name, count) -> {
            List<String> next = new ArrayList<>(folder);
            next.add(name);
            list.addRow(new FolderRow(Component.literal(humanize(name) + " (" + count + ")  >"), () -> open(List.copyOf(next))));
        });
    }

    private Map<String, Integer> childFolders(List<OptionRef> refs) {
        Map<String, Integer> children = new TreeMap<>();

        for (OptionRef ref : refs) {
            List<String> parent = ref.path.subList(0, ref.path.size() - 1);

            if (parent.size() > folder.size() && parent.subList(0, folder.size()).equals(folder)) {
                children.merge(parent.get(folder.size()), 1, Integer::sum);
            }
        }

        return children;
    }

    private static Map<String, EnumMap<Column, OptionRef>> familiesOf(List<OptionRef> refs) {
        Map<String, EnumMap<Column, OptionRef>> families = new LinkedHashMap<>();

        for (OptionRef ref : refs) {
            Matcher matcher = FAMILY.matcher(ref.lastSegment());

            if (matcher.matches() && ref.kind != OptionRef.Kind.TEXT) {
                Column column = Column.valueOf(matcher.group(2).toUpperCase(Locale.ROOT));
                families.computeIfAbsent(matcher.group(1), key -> new EnumMap<>(Column.class)).put(column, ref);
            }
        }

        families.values().removeIf(cells -> cells.size() < 2);

        return families;
    }

    private static String familyLabel(EnumMap<Column, OptionRef> cells) {
        String name = cells.values().iterator().next().name.getString();
        String stripped = CATEGORY_WORDS.matcher(name).replaceAll("").trim();

        return stripped.isEmpty() ? name : stripped;
    }

    private static String humanize(String camel) {
        if (camel.isEmpty()) {
            return camel;
        }

        String spaced = camel.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");

        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String crumb() {
        List<String> parts = new ArrayList<>();
        folder.forEach(part -> parts.add(humanize(part)));

        if (byCategory) {
            parts.add(Component.translatable("mms_settings.opac.by_category_crumb").getString());
        }

        return String.join(" > ", parts);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);

        if (writeFailed || OpacWriter.hasFailed()) {
            graphics.drawCenteredString(font, Component.translatable("mms_settings.opac.write_failed"), width / 2, height - 38, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private class MessageRow extends RowList.Row {
        private final Component message;

        MessageRow(Component message) {
            this.message = message;
        }

        @Override
        protected void paint(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            graphics.drawCenteredString(font, message, x + width / 2, y + 6, 0xFFAAAAAA);
        }
    }

    private class BackRow extends RowList.Row {
        private final Button back;
        private final String text = crumb();

        BackRow() {
            this.back = add(
                Button.builder(
                    Component.translatable("mms_settings.opac.back"),
                    button -> {
                        if (byCategory) {
                            byCategory = false;
                            refill();
                        } else {
                            open(folder.subList(0, folder.size() - 1));
                        }
                    }
                )
                    .size(60, 20)
                    .build()
            );
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            back.setX(x);
            back.setY(y);
        }

        @Override
        protected void paint(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            String fitted = font.plainSubstrByWidth(text, width - 72);
            graphics.drawString(font, fitted, x + 68, y + 6, 0xFFAAAAAA);
        }
    }

    private class FolderRow extends RowList.Row {
        private final Button button;

        FolderRow(Component text, Runnable action) {
            this.button = add(
                Button.builder(text, pressed -> action.run())
                    .size(RowList.ROW_WIDTH - 20, 20)
                    .build()
            );
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            button.setWidth(width);
            button.setX(x);
            button.setY(y);
        }
    }

    private class SwitchRow extends RowList.Row {
        private final Button button;

        SwitchRow() {
            this.button = add(
                Button.builder(Component.translatable("mms_settings.opac.showing", selected.label()), pressed -> {
                    selected = Column.values()[(selected.ordinal() + 1) % Column.values().length];
                    refill();
                })
                    .size(RowList.ROW_WIDTH - 20, 20)
                    .build()
            );
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            button.setWidth(width);
            button.setX(x);
            button.setY(y);
        }
    }

    private class OptionRow extends RowList.Row {
        private final Component label;
        private final List<AbstractWidget> controls;

        OptionRow(OptionRef ref) {
            this(ref, ref.name.getString());
        }

        OptionRow(OptionRef ref, String label) {
            this.label = Component.literal(label);
            this.controls = OptionControl.create(ref, VALUE_WIDTH, OpacScreen.this::onWriteFailed);
            controls.forEach(this::add);
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            int cursor = x + width - VALUE_WIDTH;

            for (AbstractWidget control : controls) {
                control.setX(cursor);
                control.setY(y);
                cursor += control.getWidth() + 2;
            }
        }

        @Override
        protected void paint(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            String text = font.plainSubstrByWidth(label.getString(), width - VALUE_WIDTH - 8);
            graphics.drawString(font, text, x + 4, y + 6, 0xFFFFFFFF);
        }
    }
}
