package dev.handyshulkers;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

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
            ItemStack box = new ItemStack(Items.SHULKER_BOX);

            ItemStackContainer container = new ItemStackContainer(box, 27);
            expect("read: empty box starts empty", container.getItem(0).isEmpty());

            container.setItem(0, new ItemStack(Items.DIAMOND));
            container.setItem(1, new ItemStack(Items.APPLE));
            expect("write: diamond persisted into BlockEntityTag", nbtHas(box, Items.DIAMOND));
            expect("write: apple persisted into BlockEntityTag", nbtHas(box, Items.APPLE));

            ItemStackContainer reopened = new ItemStackContainer(box, 27);
            expect("reopen: diamond visible at slot 0", reopened.getItem(0).getItem() == Items.DIAMOND);
            expect("reopen: apple visible at slot 1", reopened.getItem(1).getItem() == Items.APPLE);
            expect("reopen: slot 2 empty", reopened.getItem(2).isEmpty());

            reopened.removeItem(1, 1);
            ItemStackContainer afterRemoval = new ItemStackContainer(box, 27);
            expect("remove: apple removed cleanly", afterRemoval.getItem(1).isEmpty());
            expect("remove: diamond untouched", afterRemoval.getItem(0).getItem() == Items.DIAMOND);

            // slot order must survive the round-trip (Slot tags)
            afterRemoval.setItem(26, new ItemStack(Items.ENDER_PEARL));
            ItemStackContainer ordered = new ItemStackContainer(box, 27);
            expect("slots: last slot lands at index 26", ordered.getItem(26).getItem() == Items.ENDER_PEARL);
            expect("slots: slot 0 still diamond", ordered.getItem(0).getItem() == Items.DIAMOND);

            // container items must be refused on the box slots
            expect("safety: shulker box is a container item", ItemStackContainer.isContainerItem(new ItemStack(Items.SHULKER_BOX)));
            expect("safety: apple is not", !ItemStackContainer.isContainerItem(new ItemStack(Items.APPLE)));

            HandyShulkers.LOGGER.info("[handyshulkers] SELF-TEST PASS: container round-trip OK");
        } catch (Throwable t) {
            HandyShulkers.LOGGER.error("[handyshulkers] SELF-TEST FAIL: {}", t.toString());
        }
    }

    private static boolean nbtHas(ItemStack box, net.minecraft.item.Item item) {
        CompoundNBT tag = box.getTag();
        if (tag == null || !tag.contains("BlockEntityTag", 10)) {
            return false;
        }
        CompoundNBT blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", 9)) {
            return false;
        }
        ListNBT list = blockEntityTag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static void expect(String name, boolean condition) {
        if (!condition) {
            throw new IllegalStateException(name);
        }
        HandyShulkers.LOGGER.info("[handyshulkers] self-test: {} — ok", name);
    }
}
