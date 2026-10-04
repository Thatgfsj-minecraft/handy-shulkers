package dev.handyshulkers;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A container backed by the vanilla {@code BlockEntityTag.Items} NBT of a
 * single held ItemStack — the storage location 1.20.1 uses when a shulker box
 * is placed (the loot table copies {@code Items} into {@code BlockEntityTag}
 * on break, and placement loads it back), so every change is read by vanilla
 * and by modded boxes that follow the convention. Nothing is lost when the
 * menu is closed.
 *
 * <p>1.20.1 port note: there are no data components (1.20.5+), so the 1.21
 * container-component bridge is replaced by this NBT bridge.
 */
public class ItemStackContainer implements Container {

    protected final NonNullList<ItemStack> items;
    protected final ItemStack box;
    protected boolean dirty;

    public ItemStackContainer(ItemStack box, int size) {
        this.box = box;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        CompoundTag tag = box.getTag();
        if (tag != null && tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
            if (blockEntityTag.contains("Items", Tag.TAG_LIST)) {
                ContainerHelper.loadAllItems(blockEntityTag, items);
            }
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stack = ContainerHelper.removeItem(items, index, count);
        setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = ContainerHelper.takeItem(items, index);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {
        dirty = true;
        writeBack();
    }

    /** Writes the full slot list back into the item's BlockEntityTag.Items. */
    protected void writeBack() {
        CompoundTag tag = box.getOrCreateTag();
        CompoundTag blockEntityTag = tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)
                ? tag.getCompound("BlockEntityTag")
                : new CompoundTag();
        ContainerHelper.saveAllItems(blockEntityTag, items, true);
        tag.put("BlockEntityTag", blockEntityTag);
        box.setTag(tag);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return !isContainerItem(stack);
    }

    @Override
    public void startOpen(Player player) {
        playSound(player.level(), player.getX(), player.getY(), player.getZ(), getOpenSound());
    }

    @Override
    public void stopOpen(Player player) {
        if (dirty) {
            setChanged();
        }
        playSound(player.level(), player.getX(), player.getY(), player.getZ(), getCloseSound());
    }

    @Override
    public boolean stillValid(Player player) {
        // Identity match on purpose: comparing item contents would keep the
        // menu "valid" for an identical second box, and edits would then write
        // into a different, possibly dropped, stack.
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == box) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    protected SoundEvent getOpenSound() {
        return SoundEvents.SHULKER_BOX_OPEN;
    }

    protected SoundEvent getCloseSound() {
        return SoundEvents.SHULKER_BOX_CLOSE;
    }

    /**
     * Anything that holds other items must not go back into a box: no
     * shulker-in-shulker. The tag catches modded boxes whose empty item
     * carries no contents yet; a present BlockEntityTag.Items list catches
     * any vanilla-style container item that was mined while loaded.
     */
    static boolean isContainerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(HandyShulkers.SHULKER_BOXES) || ShulkerOpenLogic.isVanillaShulkerBox(stack)) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)
                && tag.getCompound("BlockEntityTag").contains("Items", Tag.TAG_LIST);
    }

    private static void playSound(Level level, double x, double y, double z, SoundEvent sound) {
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
