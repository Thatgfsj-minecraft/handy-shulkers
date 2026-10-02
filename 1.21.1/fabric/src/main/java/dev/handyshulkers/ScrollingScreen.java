package dev.handyshulkers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Six-row window over a large box: the mouse wheel scrolls one row per notch
 * and the scroll bar on the right can be dragged. Scroll input rides the
 * vanilla container button packet (button id = target row), so there is no
 * custom networking and the server stays authoritative.
 */
public class ScrollingScreen extends AbstractContainerScreen<ScrollingMenu> {

    public static final int TEXTURE_WIDTH = 186;
    public static final int TEXTURE_HEIGHT = 240;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HandyShulkers.MOD_ID, "textures/gui/scrolling_container.png");

    private static final int TRACK_X = 171;
    private static final int TRACK_Y = ScrollingMenu.WINDOW_Y;
    private static final int TRACK_WIDTH = 9;
    private static final int TRACK_HEIGHT = ScrollingMenu.VIEW_ROWS * 18;
    private static final int SLIDER_HEIGHT = 16;
    private static final int SLIDER_U = 0;
    private static final int SLIDER_V = 224;

    private boolean draggingScrollbar;

    public ScrollingScreen(ScrollingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 186;
        this.imageHeight = 224;
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        int maxRow = this.menu.getMaxRow();
        int sliderY = TRACK_Y;
        if (maxRow > 0) {
            sliderY += (TRACK_HEIGHT - SLIDER_HEIGHT) * this.menu.getScrollRow() / maxRow;
        }
        guiGraphics.blit(TEXTURE, this.leftPos + TRACK_X, this.topPos + sliderY,
                SLIDER_U, SLIDER_V, TRACK_WIDTH, SLIDER_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int notches = (int) Math.signum(scrollY) * Math.max(1, (int) Math.ceil(Math.abs(scrollY)));
            scrollTo(this.menu.getScrollRow() + notches);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = button == 0
                && mouseX >= this.leftPos + TRACK_X && mouseX < this.leftPos + TRACK_X + TRACK_WIDTH
                && mouseY >= this.topPos + TRACK_Y && mouseY < this.topPos + TRACK_Y + TRACK_HEIGHT;
        if (this.draggingScrollbar) {
            scrollTo(rowFromY(mouseY));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            scrollTo(rowFromY(mouseY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int rowFromY(double mouseY) {
        int maxRow = this.menu.getMaxRow();
        if (maxRow <= 0) {
            return 0;
        }
        double t = (mouseY - this.topPos - TRACK_Y - SLIDER_HEIGHT / 2.0) / (TRACK_HEIGHT - SLIDER_HEIGHT);
        return Mth.clamp((int) Math.round(t * maxRow), 0, maxRow);
    }

    private void scrollTo(int row) {
        int target = Mth.clamp(row, 0, this.menu.getMaxRow());
        if (target != this.menu.getScrollRow()) {
            int delta = target - this.menu.getScrollRow();
            int button = delta == -1 ? 0 : delta == 1 ? 1 : 100 + target;
            this.menu.clickMenuButton(this.minecraft.player, button);
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
        }
    }
}
