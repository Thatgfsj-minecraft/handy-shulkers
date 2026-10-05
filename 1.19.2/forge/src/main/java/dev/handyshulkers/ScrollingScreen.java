package dev.handyshulkers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Six-row window over a large box, visually identical to the vanilla 6-row
 * chest: the background is the vanilla generic_54 texture (same two-segment
 * blit as the vanilla container screen) and the scroll thumb reuses the
 * vanilla creative-tab scroller graphic (it lives on the tabs sheet — the
 * separate scroller sprites only arrive in 1.19.4). Scrolling is a purely
 * local viewport move — wheel steps one row, clicking the track pages a whole
 * window, dragging the thumb follows continuously; no packet is ever sent.
 *
 * <p>1.19.2 port note: there is no GuiGraphics (1.19.4+), so everything is
 * drawn straight through the PoseStack with GuiComponent's blit/fill, and the
 * texture must be bound per pass via RenderSystem.setShaderTexture.
 */
public class ScrollingScreen extends AbstractContainerScreen<ScrollingMenu> {

    /** Vanilla 6-row chest background (generic_54: v0..rows*18+17 top band, v126.. backpack area). */
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("textures/gui/container/generic_54.png");
    /** Vanilla creative scroll thumb (12x15, on the tabs sheet in 1.19.2). */
    private static final ResourceLocation SCROLLER =
            new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final int PANEL_W = 176;
    /** Vanilla panel gray (198,198,198). */
    private static final int VANILLA_GRAY = 0xFFC6C6C6;
    private static final int TRACK_X = 178, TRACK_Y = ScrollingMenu.WINDOW_Y, TRACK_W = 12, TRACK_H = 106;
    private static final int THUMB_H = 15;

    /** Mouse-y minus thumb top while dragging the slider; -1 = not dragging. */
    private double dragOffset = -1;

    public ScrollingScreen(ScrollingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        // vanilla two-segment container background (6-row window + backpack area)
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, PANEL_W, ScrollingMenu.VIEW_ROWS * 18 + 17);
        blit(poseStack, this.leftPos, this.topPos + ScrollingMenu.VIEW_ROWS * 18 + 17, 0, 126, PANEL_W, 96);
        // right panel: vanilla gray extension with black outline (top/right/bottom)
        int rx = this.leftPos + PANEL_W;
        fill(poseStack, rx, this.topPos, rx + 17, this.topPos + 1, 0xFF000000);
        fill(poseStack, rx, this.topPos + 1, rx + 17, this.topPos + 221, VANILLA_GRAY);
        fill(poseStack, rx + 16, this.topPos, rx + 17, this.topPos + 222, 0xFF000000);
        fill(poseStack, rx, this.topPos + 221, rx + 17, this.topPos + 222, 0xFF000000);
        // scroll thumb: vanilla creative scroller graphic, follows the local viewport
        RenderSystem.setShaderTexture(0, SCROLLER);
        blit(poseStack, this.leftPos + TRACK_X, this.thumbTop(), 232, 0, TRACK_W, THUMB_H);
    }

    private int thumbTop() {
        int maxRow = this.menu.getMaxRow();
        if (maxRow <= 0) {
            return this.topPos + TRACK_Y;
        }
        return this.topPos + TRACK_Y
                + this.menu.getScrollRow() * (TRACK_H - THUMB_H) / maxRow;
    }

    /** Mouse y -> viewport first row. */
    private int rowAt(double mouseY) {
        int maxRow = this.menu.getMaxRow();
        if (maxRow <= 0) {
            return 0;
        }
        return Math.round((float) (mouseY - (this.topPos + TRACK_Y) - THUMB_H / 2.0)
                / (TRACK_H - THUMB_H) * maxRow);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.menu.setScrollRowLocal(this.menu.getScrollRow() + (delta > 0 ? -1 : 1));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int tx = this.leftPos + TRACK_X, ty = this.topPos + TRACK_Y;
        if (mouseX >= tx && mouseX < tx + TRACK_W && mouseY >= ty && mouseY < ty + TRACK_H) {
            int thumb = this.thumbTop();
            if (mouseY < thumb || mouseY >= thumb + THUMB_H) {
                // track click off the thumb: page the whole window up/down
                this.menu.setScrollRowLocal(this.menu.getScrollRow()
                        + (mouseY < thumb ? -ScrollingMenu.VIEW_ROWS : ScrollingMenu.VIEW_ROWS));
            }
            this.dragOffset = mouseY - this.thumbTop();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragOffset >= 0) {
            this.menu.setScrollRowLocal(this.rowAt(mouseY - this.dragOffset));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragOffset = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
