package info.mudbourn.mmssettings.hub;

import java.util.List;

import info.mudbourn.mmssettings.MmsSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// The Adventurer Tools section: MMS Dungeons menus for guilds, quests and contracts.
public class AdventurerToolsScreen extends Screen {
    private static final int COLUMNS = 2;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public AdventurerToolsScreen(Screen parent) {
        super(Component.translatable("mms_settings.adventurer.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
        GridLayout.RowHelper rows = grid.createRowHelper(COLUMNS);
        List<Targets.Tile> tiles = Targets.adventurerTools();
        for (int index = 0; index < tiles.size(); index++) {
            boolean lastOfOdd = index == tiles.size() - 1 && tiles.size() % COLUMNS != 0;
            Targets.Tile tile = tiles.get(index);
            Button.Builder builder = Button.builder(tile.label(), pressed -> open(tile));
            if (lastOfOdd) {
                builder.width(Button.DEFAULT_WIDTH * COLUMNS + 8);
            }
            rows.addChild(builder.build(), lastOfOdd ? COLUMNS : 1);
        }
        layout.addToContents(grid);

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, pressed -> onClose()).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    private void open(Targets.Tile tile) {
        try {
            minecraft.gui.setScreen(tile.factory().apply(this));
        } catch (Throwable error) {
            MmsSettings.LOGGER.warn("Could not open {}", tile.label().getString(), error);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
