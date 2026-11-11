package info.mudbourn.mmssettings.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

public class RowList extends ContainerObjectSelectionList<RowList.Row> {
    public static final int ROW_WIDTH = 340;

    public RowList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    public void addRow(Row row) {
        addEntry(row);
    }

    public void clearRows() {
        clearEntries();
        setScrollAmount(0);
    }

    @Override
    public int getRowWidth() {
        return Math.min(width - 40, ROW_WIDTH);
    }

    public abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        protected final List<AbstractWidget> widgets = new ArrayList<>();

        protected <T extends AbstractWidget> T add(T widget) {
            widgets.add(widget);
            return widget;
        }

        protected void layout(int x, int y, int width, int height) {
        }

        protected void paint(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = getContentX();
            int y = getContentY();
            int w = getContentWidth();
            int h = getContentHeight();
            layout(x, y, w, h);
            paint(graphics, x, y, w, h, mouseX, mouseY, hovered);
            for (AbstractWidget widget : widgets) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return widgets;
        }
    }
}
