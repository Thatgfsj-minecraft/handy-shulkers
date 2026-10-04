package dev.handyshulkers;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Scrolling menu for boxes larger than 54 slots, following the Sophisticated
 * Storage school: the menu permanently exposes every backing slot (slot i ->
 * backing slot i, never remapped), and scrolling is purely a client-side
 * viewport move. The client therefore holds the full content (one big sync on
 * open, then vanilla's per-slot diffing), so scrolling is instant, needs
 * no packets and no server round-trip, shift-click covers the whole container,
 * and quickcraft can never land on a remapped slot.
 *
 * <p>The backing container is always {@link #STORAGE_SLOTS} big — the 256-slot
 * NBT storage hard limit rounded up to whole rows. The playable capacity
 * itself (e.g. 243) arrives through a synced data slot; cells at or beyond it
 * are locked empty slots and stay hidden.
 */
public class ScrollingMenu extends AbstractContainerMenu {

    public static final int VIEW_ROWS = 6;
    /** Every backing slot the menu exposes: 256 (storage hard limit) rounded up to whole rows. */
    public static final int STORAGE_SLOTS = 261;
    public static final int PLAYER_INVENTORY_SLOTS = 27;
    public static final int HOTBAR_SLOTS = 9;
    /** Total menu slots: storage slots + 36 player inventory slots. */
    public static final int TOTAL_SLOTS = STORAGE_SLOTS + PLAYER_INVENTORY_SLOTS + HOTBAR_SLOTS;

    public static final int WINDOW_X = 8;
    public static final int WINDOW_Y = 18;
    public static final int PLAYER_INV_Y = 139;
    public static final int HOTBAR_Y = 197;
    private static final int HIDDEN_Y = -1000;

    /** Assigned by the loader entry while registering the menu type. */
    public static MenuType<ScrollingMenu> TYPE;

    private final Container backing;
    private final ItemStack box;
    private final DataSlot capacitySlot = DataSlot.standalone();
    /** Viewport state. Server side this stays 0 and is never read for logic. */
    private int scrollRow;

    public ScrollingMenu(int id, Inventory playerInventory, Container backing, int capacity, ItemStack box) {
        super(TYPE, id);
        this.backing = backing;
        this.box = box;
        this.capacitySlot.set(capacity);
        addDataSlot(this.capacitySlot);
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            int row = i / 9;
            addSlot(new StorageSlot(i, WINDOW_X + (i % 9) * 18,
                    row < VIEW_ROWS ? WINDOW_Y + row * 18 : HIDDEN_Y));
        }
        for (int i = 0; i < PLAYER_INVENTORY_SLOTS; i++) {
            addSlot(new Slot(playerInventory, i + 9, WINDOW_X + (i % 9) * 18, PLAYER_INV_Y + (i / 9) * 18));
        }
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            addSlot(new Slot(playerInventory, i, WINDOW_X + i * 18, HOTBAR_Y));
        }
    }

    /**
     * Client-side factory over a dummy container: vanilla slot sync aligns by
     * index, so the client holds a copy of the entire content after the open
     * broadcast and never needs to ask the server about scrolling.
     */
    public static ScrollingMenu clientCreate(int id, Inventory playerInventory) {
        return new ScrollingMenu(id, playerInventory, new SimpleContainer(STORAGE_SLOTS), 0, ItemStack.EMPTY);
    }

    /** Backing capacity in slots; synced to the client for the scroll bar. */
    public int getCapacity() {
        return capacitySlot.get();
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public int getMaxRow() {
        return Math.max(0, (getCapacity() + 8) / 9 - VIEW_ROWS);
    }

    /**
     * Client-only viewport move (called by the screen; the server never learns
     * about scrolling). Repositions the storage slots: rows inside the window
     * move into the 6-row grid, everything else parks far off-panel. The slot
     * objects are replaced, not mutated — Slot coordinates are final — and the
     * fresh instances read the same local content, so nothing flickers.
     */
    public void setScrollRowLocal(int newRow) {
        int target = Mth.clamp(newRow, 0, getMaxRow());
        if (target == scrollRow) {
            return;
        }
        scrollRow = target;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            int row = i / 9;
            int y = row >= scrollRow && row < scrollRow + VIEW_ROWS
                    ? WINDOW_Y + (row - scrollRow) * 18
                    : HIDDEN_Y;
            slots.set(i, new StorageSlot(i, WINDOW_X + (i % 9) * 18, y));
        }
    }

    /**
     * A storage cell: invisible (and unclickable, per vanilla isActive checks)
     * outside the viewport rows, and permanently locked at or beyond the
     * playable capacity — the storage cannot hold anything past its 256-slot
     * limit.
     */
    private class StorageSlot extends Slot {

        private final int storageIndex;

        private StorageSlot(int storageIndex, int x, int y) {
            super(backing, storageIndex, x, y);
            this.storageIndex = storageIndex;
            // Slot.index is assigned by addSlot for the instances it tracks;
            // the viewport relayout swaps instances in place and MUST restore
            // it, or every client click after scrolling reports slot 0.
            this.index = storageIndex;
        }

        private boolean inView() {
            int row = storageIndex / 9;
            return row >= scrollRow && row < scrollRow + VIEW_ROWS;
        }

        private boolean playable() {
            return storageIndex < getCapacity();
        }

        @Override
        public boolean isActive() {
            return inView() && playable();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return playable() && !ItemStackContainer.isContainerItem(stack);
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        // the opened box itself can never be moved into its own inventory
        if (slotIndex >= 0 && slotIndex < STORAGE_SLOTS && !box.isEmpty() && slots.get(slotIndex).getItem() == box) {
            return;
        }
        super.clicked(slotIndex, button, clickType, player);
    }

    /**
     * Standard two-segment shift-click across the WHOLE container (including
     * currently hidden rows), matching every mainstream large-container mod.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (index < STORAGE_SLOTS) {
            if (!this.moveItemStackTo(current, STORAGE_SLOTS, TOTAL_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(current, 0, STORAGE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        // ItemStackContainer's identity check keeps the menu bound to the
        // exact opened stack; the client dummy container always validates.
        return this.backing.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.backing.stopOpen(player);
    }
}
