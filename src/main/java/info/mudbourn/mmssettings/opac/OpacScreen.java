package info.mudbourn.mmssettings.opac;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final int ROW_HEIGHT = 24;
    private static final int VALUE_WIDTH = 130;
    private static final int CATEGORY_WIDTH = 60;
    private static final int CATEGORY_GAP = 2;

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
    private final Set<String> expanded = new HashSet<>();
    private ConfigKind kind = ConfigKind.MY;
    private String subConfig;
    private boolean alphabetical;
    private String search = "";
    private String openSection;
    private String openFamily;
    private Column openColumn;
    private EditBox searchBox;
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
                closeCategory();
                rebuildWidgets();
            })
                .bounds(left, 22, third, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(subLabel(), button -> {
                cycleSubConfig();
                closeCategory();
                rebuildWidgets();
            })
                .bounds(left + third + 4, 22, third, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable(alphabetical ? "mms_settings.opac.sort_alpha" : "mms_settings.opac.sort_grouped"),
                button -> {
                    alphabetical = !alphabetical;
                    rebuildWidgets();
                }
            )
                .bounds(left + (third + 4) * 2, 22, third, 20)
                .build()
        );

        searchBox = new EditBox(font, left, 46, RowList.ROW_WIDTH, 20, Component.translatable("mms_settings.search"));
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
                Component.translatable("mms_settings.opac.original"),
                button -> kind.openOriginal(manager, this, parent)
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

    private void closeCategory() {
        openSection = null;
        openFamily = null;
        openColumn = null;
    }

    private void openCategory(String section, String family, Column column) {
        openSection = section;
        openFamily = family;
        openColumn = column;
        refill();
    }

    private void refill() {
        list.clearRows();
        List<OptionRef> refs = visibleOptions();

        if (openColumn != null) {
            fillCategory(refs);

            return;
        }

        if (refs.isEmpty()) {
            list.addRow(new MessageRow(Component.translatable("mms_settings.opac.empty")));

            return;
        }

        if (alphabetical) {
            refs.sort(Comparator.comparing(ref -> ref.name.getString().toLowerCase(Locale.ROOT)));
            refs.forEach(ref -> list.addRow(new OptionRow(ref, ref.name)));

            return;
        }

        Map<String, List<OptionRef>> sections = sectionsOf(refs);
        boolean searching = !search.isBlank();

        for (Map.Entry<String, List<OptionRef>> section : sections.entrySet()) {
            boolean open = searching || expanded.contains(section.getKey());
            list.addRow(new HeaderRow(section.getKey(), section.getValue().size(), open));

            if (open) {
                addSection(section.getKey(), section.getValue(), searching);
            }
        }
    }

    private void fillCategory(List<OptionRef> refs) {
        List<OptionRef> section = sectionsOf(refs).getOrDefault(openSection, List.of());
        List<OptionRef> members = section.stream()
            .filter(ref -> belongs(ref, openFamily, openColumn))
            .toList();
        String crumb = sectionTitle(openSection) + " > " + humanize(openFamily) + " > " + openColumn.label().getString();

        list.addRow(new BackRow(Component.literal(crumb)));

        if (members.isEmpty()) {
            list.addRow(new MessageRow(Component.translatable("mms_settings.opac.empty")));
        }

        members.forEach(ref -> list.addRow(new OptionRow(ref, ref.name)));
    }

    private static boolean belongs(OptionRef ref, String family, Column column) {
        String segment = ref.lastSegment();
        String name = column.key.substring(0, 1).toUpperCase(Locale.ROOT) + column.key.substring(1);

        return segment.startsWith(family) && segment.contains(name);
    }

    private static Map<String, List<OptionRef>> sectionsOf(List<OptionRef> refs) {
        Map<String, List<OptionRef>> sections = new LinkedHashMap<>();

        for (OptionRef ref : refs) {
            sections.computeIfAbsent(ref.section(), key -> new ArrayList<>()).add(ref);
        }

        return sections;
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

    private void addSection(String key, List<OptionRef> refs, boolean searching) {
        Map<String, EnumMap<Column, OptionRef>> families = searching ? Map.of() : familiesOf(refs);

        for (OptionRef ref : refs) {
            boolean grouped = families.entrySet().stream()
                .anyMatch(family -> family.getValue().keySet().stream().anyMatch(column -> belongs(ref, family.getKey(), column)));

            if (!grouped) {
                list.addRow(new OptionRow(ref, ref.name));
            }
        }

        families.forEach((base, cells) -> list.addRow(new FamilyRow(key, base, cells.keySet())));
    }

    private static String humanize(String camel) {
        if (camel.isEmpty()) {
            return camel;
        }

        String spaced = camel.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");

        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String sectionTitle(String key) {
        if (key.isEmpty()) {
            return Component.translatable("mms_settings.opac.general").getString();
        }

        List<String> parts = new ArrayList<>();

        for (String part : key.split("\\.")) {
            parts.add(humanize(part));
        }

        return String.join(" / ", parts);
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

    private class HeaderRow extends RowList.Row {
        private final Button toggle;

        HeaderRow(String key, int count, boolean open) {
            String prefix = open ? "- " : "+ ";
            this.toggle = add(
                Button.builder(Component.literal(prefix + sectionTitle(key) + " (" + count + ")"), button -> {
                    if (!expanded.remove(key)) {
                        expanded.add(key);
                    }

                    refill();
                })
                    .size(RowList.ROW_WIDTH - 20, 20)
                    .build()
            );
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            toggle.setWidth(width);
            toggle.setX(x);
            toggle.setY(y);
        }
    }

    private class OptionRow extends RowList.Row {
        private final Component label;
        private final List<AbstractWidget> controls;

        OptionRow(OptionRef ref, Component label) {
            this.label = label;
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

    private class BackRow extends RowList.Row {
        private final Button back;
        private final Component crumb;

        BackRow(Component crumb) {
            this.crumb = crumb;
            this.back = add(
                Button.builder(Component.translatable("mms_settings.opac.back"), button -> {
                    closeCategory();
                    refill();
                })
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
            graphics.drawString(font, font.plainSubstrByWidth(crumb.getString(), width - 72), x + 68, y + 6, 0xFFFFFFFF);
        }
    }

    private class FamilyRow extends RowList.Row {
        private final String label;
        private final Map<Column, Button> buttons = new EnumMap<>(Column.class);

        FamilyRow(String section, String family, Set<Column> columns) {
            this.label = humanize(family);
            columns.forEach(column -> buttons.put(column, add(
                Button.builder(column.label(), button -> openCategory(section, family, column))
                    .size(CATEGORY_WIDTH, 20)
                    .build()
            )));
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            int start = x + width - (CATEGORY_WIDTH + CATEGORY_GAP) * Column.values().length + CATEGORY_GAP;

            buttons.forEach((column, button) -> {
                button.setX(start + column.ordinal() * (CATEGORY_WIDTH + CATEGORY_GAP));
                button.setY(y);
            });
        }

        @Override
        protected void paint(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            int room = width - (CATEGORY_WIDTH + CATEGORY_GAP) * Column.values().length - 4;
            graphics.drawString(font, font.plainSubstrByWidth(label, room), x + 4, y + 6, 0xFFFFFFFF);
        }
    }
}
