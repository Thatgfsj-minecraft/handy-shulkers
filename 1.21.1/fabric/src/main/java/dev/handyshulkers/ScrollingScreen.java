package dev.handyshulkers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Six-row window over a large box. Scrolling is a purely local viewport move
 * (the menu already holds the whole content), so the wheel and the scroll bar
 * never send a packet: the wheel steps one row per notch, clicking the track
 * jumps, and dragging the slider follows continuously with a grab offset.
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

    /** Rows between the grab point and the slider top while dragging; -1 = not dragging. */
    private int dragGrabOffset = -1;

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
        guiGraphics.blit(TEXTURE, this.leftPos + TRACK_X, this.topPos + sliderY(),
                SLIDER_U, SLIDER_V, TRACK_WIDTH, SLIDER_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private int sliderY() {
        int maxRow = this.menu.getMaxRow();
        if (maxRow <= 0) {
            return TRACK_Y;
        }
        return TRACK_Y + (TRACK_HEIGHT - SLIDER_HEIGHT) * this.menu.getScrollRow() / maxRow;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int notches = (int) Math.signum(scrollY) * Math.max(1, (int) Math.ceil(Math.abs(scrollY)));
            this.menu.setScrollRowLocal(this.menu.getScrollRow() + notches);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= this.leftPos + TRACK_X && mouseX < this.leftPos + TRACK_X + TRACK_WIDTH
                && mouseY >= this.topPos + TRACK_Y && mouseY < this.topPos + TRACK_Y + TRACK_HEIGHT) {
            int rowUnder = rowFromY(mouseY);
            int sliderY = sliderY();
            boolean onSlider = mouseY >= this.topPos + sliderY && mouseY < this.topPos + sliderY + SLIDER_HEIGHT;
            this.dragGrabOffset = onSlider ? rowUnder - this.menu.getScrollRow() : 0;
            this.menu.setScrollRowLocal(rowUnder - this.dragGrabOffset);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragGrabOffset >= 0) {
            this.menu.setScrollRowLocal(rowFromY(mouseY) - this.dragGrabOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragGrabOffset = -1;
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
}
