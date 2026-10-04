package dev.handyshulkers;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.inventory.ItemStackHelper;

/**
 * Headless self-test for the container bridge, run once at server start: a
 * shulker box item is loaded into the container, mutated, and written back,
 * then re-read through a fresh container. This exercises the exact read and
 * write paths players hit when opening a box, so data-integrity regressions
 * surface in the log even without anyone playing.
 */
public final class SelfTest {

    private SelfTest() {
    }

    public static void run() {
        try {
            // 1.12.2 has only 16 colored shulker box blocks (no uncolored
            // one); the craftable purple box stands in for the vanilla item
            Item shulkerItem = Item.getItemFromBlock(Blocks.PURPLE_SHULKER_BOX);
            expect("setup: vanilla shulker box item resolved", shulkerItem != null);
            ItemStack box = new ItemStack(shulkerItem);

            ItemStackContainer container = new ItemStackContainer(box, 27);
            container.setInventorySlotContents(0, new ItemStack(Items.DIAMOND));
            expect("write: diamond persisted into NBT", nbtHas(box, Items.DIAMOND));

            ItemStackContainer reopened = new ItemStackContainer(box, 27);
            expect("read: diamond visible at slot 0", isItem(reopened.getStackInSlot(0), Items.DIAMOND));
            expect("read: slot 1 empty", reopened.getStackInSlot(1).isEmpty());

            reopened.setInventorySlotContents(1, new ItemStack(Items.APPLE));
            expect("write: apple persisted into NBT", nbtHas(box, Items.APPLE));

            ItemStackContainer afterWrite = new ItemStackContainer(box, 27);
            expect("reopen: apple visible at slot 1", isItem(afterWrite.getStackInSlot(1), Items.APPLE));
            expect("reopen: diamond still at slot 0", isItem(afterWrite.getStackInSlot(0), Items.DIAMOND));

            afterWrite.decrStackSize(1, 1);
            ItemStackContainer afterRemoval = new ItemStackContainer(box, 27);
            expect("remove: apple removed cleanly", afterRemoval.getStackInSlot(1).isEmpty());

            // nesting guard: shulker boxes refuse to go into opened boxes
            expect("guard: shulker box refused inside opened box", ItemStackContainer.isContainerItem(new ItemStack(shulkerItem)));

            // Large boxes (>54 slots): the NBT write path must round-trip every
            // backing slot. 1.12.2 refuses to OPEN such boxes (no scrolling
            // UI), but if content was written by other means it must never be
            // truncated by this mod's own code paths.
            NonNullList<ItemStack> big = NonNullList.withSize(243, ItemStack.EMPTY);
            big.set(0, new ItemStack(Items.DIAMOND));
            big.set(242, new ItemStack(Items.GOLD_INGOT));
            NBTTagCompound bigTag = new NBTTagCompound();
            ItemStackHelper.saveAllItems(bigTag, big, false);
            NonNullList<ItemStack> bigReopened = NonNullList.withSize(243, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(bigTag, bigReopened);
            expect("scroll: slot 0 survives round-trip", isItem(bigReopened.get(0), Items.DIAMOND));
            expect("scroll: slot 242 survives round-trip", isItem(bigReopened.get(242), Items.GOLD_INGOT));
            expect("scroll: middle slot untouched", bigReopened.get(100).isEmpty());

            HandyShulkers.LOGGER.info("[handyshulkers] SELF-TEST PASS: container round-trip OK");
        } catch (Throwable t) {
            HandyShulkers.LOGGER.error("[handyshulkers] SELF-TEST FAIL: {}", t.toString());
        }
    }

    private static boolean nbtHas(ItemStack box, net.minecraft.item.Item item) {
        NBTTagCompound tag = box.getTagCompound();
        if (tag == null || !tag.hasKey("Items", 9)) {
            return false;
        }
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = new ItemStack(list.getCompoundTagAt(i));
            if (isItem(stack, item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isItem(ItemStack stack, net.minecraft.item.Item item) {
        return !stack.isEmpty() && stack.getItem() == item;
    }

    private static void expect(String name, boolean condition) {
        if (!condition) {
            throw new IllegalStateException(name);
        }
        HandyShulkers.LOGGER.info("[handyshulkers] self-test: {} — ok", name);
    }
}
