package dev.handyshulkers;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * A container backed by the vanilla {@link net.minecraft.world.item.component.ItemContainerContents}
 * data component of a single held ItemStack. Every change is written straight back
 * into the item, so nothing is lost when the menu is closed.
 */
public class ItemStackContainer implements Container {

    protected final NonNullList<ItemStack> items;
    protected final ItemStack box;
    protected boolean dirty;

    public ItemStackContainer(ItemStack box, int size) {
        this.box = box;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        ItemContainerContents contents = box.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(items);
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
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return !isContainerItem(stack);
    }

    @Override
    public void startOpen(ContainerUser user) {
        LivingEntity entity = user.getLivingEntity();
        playSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), getOpenSound());
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (dirty) {
            setChanged();
        }
        LivingEntity entity = user.getLivingEntity();
        playSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), getCloseSound());
    }

    @Override
    public boolean stillValid(Player player) {
        // Identity match on purpose: Inventory.contains uses component equality,
        // so an identical second box would keep the menu "valid" and edits would
        // then write into a different, possibly dropped, stack.
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

    static boolean isContainerItem(ItemStack stack) {
        return stack.has(DataComponents.CONTAINER) || stack.is(HandyShulkers.SHULKER_BOXES);
    }

    private static void playSound(Level level, double x, double y, double z, SoundEvent sound) {
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
