package info.mudbourn.mmssettings.hub;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import info.mudbourn.mmssettings.MmsSettings;
import info.mudbourn.mmssettings.ui.RowList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class AllModsScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private enum Tab {
        MODS("mms_settings.hub.tab_mods"),
        FILES("mms_settings.hub.tab_files");

        final String key;

        Tab(String key) {
            this.key = key;
        }

        Tab next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final Screen parent;
    private Tab tab = Tab.MODS;
    private String search = "";
    private EditBox searchBox;
    private RowList list;
    private List<ModMenuSources.Entry> modEntries = List.of();
    private List<ConfigFiles.Entry> fileEntries = List.of();

    public AllModsScreen(Screen parent) {
        super(Component.translatable("mms_settings.all.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        modEntries = MmsSettings.modMenuLoaded() ? ModMenuSources.collect() : List.of();
        fileEntries = ConfigFiles.collect();

        int left = width / 2 - RowList.ROW_WIDTH / 2;
        addRenderableWidget(
            Button.builder(Component.translatable(tab.key), button -> {
                tab = tab.next();
                rebuildWidgets();
            })
                .bounds(left, 22, 100, 20)
                .build()
        );

        searchBox = new EditBox(font, left + 104, 22, RowList.ROW_WIDTH - 104, 20, Component.translatable("mms_settings.search"));
        searchBox.setHint(Component.translatable("mms_settings.search"));
        searchBox.setValue(search);
        searchBox.setResponder(text -> {
            search = text;
            refill();
        });
        addRenderableWidget(searchBox);

        list = addRenderableWidget(new RowList(minecraft, width, height - 76, 48, ROW_HEIGHT));

        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20)
                .build()
        );

        refill();
        setInitialFocus(searchBox);
    }

    private void refill() {
        list.clearRows();
        String needle = search.toLowerCase(Locale.ROOT).trim();

        if (tab == Tab.MODS) {
            for (ModMenuSources.Entry entry : modEntries) {
                if (matches(needle, entry.name(), entry.id())) {
                    list.addRow(new LinkRow(entry.name(), entry.id(), () -> open(entry.factory())));
                }
            }
        } else {
            for (ConfigFiles.Entry entry : fileEntries) {
                if (matches(needle, entry.relative(), entry.relative())) {
                    list.addRow(new LinkRow(entry.relative(), "", () -> openFile(entry.path())));
                }
            }
        }
    }

    private static boolean matches(String needle, String first, String second) {
        return needle.isEmpty()
            || first.toLowerCase(Locale.ROOT).contains(needle)
            || second.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void open(Function<Screen, Screen> factory) {
        try {
            minecraft.setScreen(factory.apply(this));
        } catch (Throwable error) {
            MmsSettings.LOGGER.warn("Config screen failed to open", error);
        }
    }

    private static void openFile(Path path) {
        Util.getPlatform().openFile(path.toFile());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private class LinkRow extends RowList.Row {
        private final String label;
        private final String detail;
        private final Button open;

        LinkRow(String label, String detail, Runnable action) {
            this.label = label;
            this.detail = detail;
            this.open = add(
                Button.builder(Component.translatable("mms_settings.hub.open"), button -> action.run())
                    .size(60, 20)
                    .build()
            );
        }

        @Override
        protected void layout(int x, int y, int width, int height) {
            open.setX(x + width - 60);
            open.setY(y);
        }

        @Override
        protected void paint(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            int available = width - 70;
            graphics.drawString(font, font.plainSubstrByWidth(label, available), x + 2, y + (detail.isEmpty() ? 6 : 1), 0xFFFFFFFF);

            if (!detail.isEmpty()) {
                graphics.drawString(font, font.plainSubstrByWidth(detail, available), x + 2, y + 11, 0xFF888888);
            }
        }
    }
}
