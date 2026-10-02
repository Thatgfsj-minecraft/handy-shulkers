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
 * Scrolling menu for boxes larger than 54 slots: the 54 window slots are a
 * moving 6-row view over the backing container (offset = scroll row * 9), so
 * every slot of even a 243-slot box stays reachable and nothing can be lost
 * on write-back. Scrolling rides the vanilla container button packet (the
 * lectern page-turn mechanism, button id = target row); the scroll row and
 * the backing capacity are synced data slots.
 *
 * <p>The client builds the same menu over a dummy 54-slot container — the
 * server computes the window and broadcasts slot contents, so the client
 * never rebuilds and there is no custom networking.
 */
public class ScrollingMenu extends AbstractContainerMenu {

    public static final int VIEW_ROWS = 6;
    public static final int WINDOW_SLOTS = VIEW_ROWS * 9;
    public static final int PLAYER_INVENTORY_SLOTS = 27;
    public static final int HOTBAR_SLOTS = 9;
    /** Total menu slots: 54 window slots + 36 player inventory slots. */
    public static final int TOTAL_SLOTS = WINDOW_SLOTS + PLAYER_INVENTORY_SLOTS + HOTBAR_SLOTS;

    public static final int WINDOW_X = 8;
    public static final int WINDOW_Y = 18;
    public static final int PLAYER_INV_Y = 140;
    public static final int HOTBAR_Y = 198;

    /** Assigned by the loader entry while registering the menu type. */
    public static MenuType<ScrollingMenu> TYPE;

    private final Container backing;
    private final ItemStack box;
    private final int playable;
    private final DataSlot scrollRow = DataSlot.standalone();
    private final DataSlot capacitySlot = DataSlot.standalone();

    public ScrollingMenu(int id, Inventory playerInventory, Container backing, int capacity, ItemStack box) {
        super(TYPE, id);
        this.backing = backing;
        this.box = box;
        this.playable = capacity;
        this.capacitySlot.set(capacity);
        addDataSlot(this.scrollRow);
        addDataSlot(this.capacitySlot);
        rebuildWindow();
        for (int i = 0; i < PLAYER_INVENTORY_SLOTS; i++) {
            addSlot(new Slot(playerInventory, i + 9, WINDOW_X + (i % 9) * 18, PLAYER_INV_Y + (i / 9) * 18));
        }
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            addSlot(new Slot(playerInventory, i, WINDOW_X + i * 18, HOTBAR_Y));
        }
    }

    /**
     * Client-side factory: a fixed 54-slot dummy view. Contents arrive through
     * the normal container sync packets, the scroll row and capacity through
     * the data slots; neither the slots nor the backing container are used
     * for anything server-authoritative on this side.
     */
    public static ScrollingMenu clientCreate(int id, Inventory playerInventory) {
        return new ScrollingMenu(id, playerInventory, new SimpleContainer(WINDOW_SLOTS), WINDOW_SLOTS, ItemStack.EMPTY);
    }

    /**
     * Visible window over the backing container: slot i shows backing[i + row * 9].
     * The backing container is padded to whole rows; cells beyond the playable
     * capacity (e.g. a declared 256-slot box pads to 29 rows) render as locked
     * empty cells — the container component itself tops out at 256 slots, so
     * content there could never save.
     */
    private void rebuildWindow() {
        int offset = scrollRow.get() * 9;
        if (!slots.isEmpty()) {
            slots.subList(0, WINDOW_SLOTS).clear();
        }
        for (int i = 0; i < WINDOW_SLOTS; i++) {
            slots.add(windowSlot(offset + i, WINDOW_X + (i % 9) * 18, WINDOW_Y + (i / 9) * 18));
        }
    }

    private Slot windowSlot(int backingIndex, int x, int y) {
        return new Slot(backing, backingIndex, x, y) {
            private boolean playable() {
                return backingIndex < ScrollingMenu.this.playable;
            }

            @Override
            public ItemStack getItem() {
                return playable() ? super.getItem() : ItemStack.EMPTY;
            }

            @Override
            public void set(ItemStack stack) {
                if (playable()) {
                    super.set(stack);
                }
            }

            @Override
            public boolean hasItem() {
                return playable() && super.hasItem();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return playable() && !ItemStackContainer.isContainerItem(stack);
            }
        };
    }

    /**
     * Vanilla container button packet (lectern page-turn mechanism):
     * button 0 = scroll up one row, 1 = scroll down one row, 100 + n = jump
     * to row n — the same protocol the compressed-blocks scrolling menu
     * speaks. On the client this only moves the data slot for instant
     * scroll-bar feedback; on the server it re-points the window slots and
     * the changed contents are broadcast through the normal sync path.
     */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        int target;
        if (button == 0) {
            target = scrollRow.get() - 1;
        } else if (button == 1) {
            target = scrollRow.get() + 1;
        } else if (button >= 100) {
            target = button - 100;
        } else {
            return false;
        }
        target = Mth.clamp(target, 0, getMaxRow());
        if (target == scrollRow.get()) {
            return false;
        }
        scrollRow.set(target);
        if (!player.level().isClientSide()) {
            rebuildWindow();
            broadcastFullState();
        }
        return true;
    }

    public int getScrollRow() {
        return scrollRow.get();
    }

    /** Backing capacity in slots; synced to the client for the scroll bar. */
    public int getCapacity() {
        return capacitySlot.get();
    }

    public int getMaxRow() {
        return Math.max(0, (getCapacity() + 8) / 9 - VIEW_ROWS);
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        // the opened box itself can never be moved into its own inventory
        if (slotIndex >= 0 && slotIndex < WINDOW_SLOTS && !box.isEmpty() && slots.get(slotIndex).getItem() == box) {
            return;
        }
        super.clicked(slotIndex, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (index < WINDOW_SLOTS) {
            if (!this.moveItemStackTo(current, WINDOW_SLOTS, TOTAL_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(current, 0, WINDOW_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
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
