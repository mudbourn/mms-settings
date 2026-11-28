package info.mudbourn.mmssettings.hub;

import net.fabricmc.loader.api.FabricLoader;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.util.List;

import info.mudbourn.mmssettings.MmsSettings;
import info.mudbourn.mmssettings.MmsSettingsClient;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class HubScreen extends Screen {
    private static final int COLUMNS = 2;
    private static final int FOOTER_SPACING = 8;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public HubScreen(Screen parent) {
        super(Component.translatable("mms_settings.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
        GridLayout.RowHelper rows = grid.createRowHelper(COLUMNS);
        List<Targets.Tile> tiles = Targets.resolve();
        for (int index = 0; index < tiles.size(); index++) {
            boolean lastOfOdd = index == tiles.size() - 1 && tiles.size() % COLUMNS != 0;
            rows.addChild(tileButton(tiles.get(index), lastOfOdd), lastOfOdd ? COLUMNS : 1);
        }
        layout.addToContents(grid);

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_SPACING));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            footer.addChild(Button.builder(
                Component.translatable("mms_settings.hub.all_mods"),
                pressed -> minecraft.gui.setScreen(ModMenuApi.createModsScreen(this))
            ).build());
        }
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, pressed -> onClose()).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    // Builds a tile button, as wide as both columns when full width.
    private Button tileButton(Targets.Tile tile, boolean fullWidth) {
        Button.Builder builder = Button.builder(tile.label(), pressed -> open(tile));
        if (fullWidth) {
            builder.width(Button.DEFAULT_WIDTH * COLUMNS + 8);
        }
        Button button = builder.build();
        if (tile.needsWorld() && minecraft.level == null) {
            button.active = false;
            button.setTooltip(Tooltip.create(Component.translatable("mms_settings.hub.needs_world")));
        }
        return button;
    }

    private void open(Targets.Tile tile) {
        try {
            minecraft.gui.setScreen(tile.factory().apply(this));
        } catch (Throwable error) {
            MmsSettings.LOGGER.warn("Could not open {}", tile.label().getString(), error);
        }
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
        minecraft.gui.setScreen(parent);
    }
}
