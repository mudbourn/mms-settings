package info.mudbourn.mmssettings.hub;

import java.util.List;

import info.mudbourn.mmssettings.MmsSettings;
import info.mudbourn.mmssettings.MmsSettingsClient;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class HubScreen extends Screen {
    private static final int TILE_WIDTH = 160;
    private static final int TILE_HEIGHT = 28;
    private static final int GAP = 6;
    private static final int TOP = 36;

    private final Screen parent;

    public HubScreen(Screen parent) {
        super(Component.translatable("mms_settings.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<Targets.Tile> tiles = Targets.resolve();
        int left = width / 2 - TILE_WIDTH - GAP / 2;
        int index = 0;

        for (Targets.Tile tile : tiles) {
            int x = left + (index % 2) * (TILE_WIDTH + GAP);
            int y = TOP + (index / 2) * (TILE_HEIGHT + GAP);
            Button button = Button.builder(tile.label(), pressed -> open(tile))
                .bounds(x, y, TILE_WIDTH, TILE_HEIGHT)
                .build();

            if (tile.needsWorld() && minecraft.level == null) {
                button.active = false;
                button.setTooltip(Tooltip.create(Component.translatable("mms_settings.hub.needs_world")));
            }

            addRenderableWidget(button);
            index++;
        }

        addRenderableWidget(
            Button.builder(
                Component.translatable("mms_settings.hub.all_mods"),
                pressed -> minecraft.setScreen(new AllModsScreen(this))
            )
                .bounds(width / 2 - 100, height - 50, 200, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, pressed -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20)
                .build()
        );
    }

    private void open(Targets.Tile tile) {
        try {
            minecraft.setScreen(tile.factory().apply(this));
        } catch (Throwable error) {
            MmsSettings.LOGGER.warn("Could not open {}", tile.label().getString(), error);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Key mappings don't tick while a screen is open, so match the open bind here to close.
        if (MmsSettingsClient.openHubKey().matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
