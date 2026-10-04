package dev.handyshulkers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Headless self-test for the container bridge, run once at server start: a
 * shulker box item is loaded into the container, mutated, and written back,
 * then re-read through a fresh container. This exercises the exact read and
 * write paths players hit when opening a box, so data-integrity regressions
 * surface in the log even without anyone playing.
 *
 * <p>1.20.1 port note: assertions run against the BlockEntityTag.Items NBT,
 * the storage location this version reads and writes.
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
            expect("reopen: diamond visible at slot 0", reopened.getItem(0).is(Items.DIAMOND));
            expect("reopen: apple visible at slot 1", reopened.getItem(1).is(Items.APPLE));
            expect("reopen: slot 2 empty", reopened.getItem(2).isEmpty());

            reopened.removeItem(1, 1);
            ItemStackContainer afterRemoval = new ItemStackContainer(box, 27);
            expect("remove: apple removed cleanly", afterRemoval.getItem(1).isEmpty());
            expect("remove: diamond untouched", afterRemoval.getItem(0).is(Items.DIAMOND));

            // slot order must survive the round-trip (Slot tags)
            afterRemoval.setItem(26, new ItemStack(Items.ENDER_PEARL));
            ItemStackContainer ordered = new ItemStackContainer(box, 27);
            expect("slots: last slot lands at index 26", ordered.getItem(26).is(Items.ENDER_PEARL));
            expect("slots: slot 0 still diamond", ordered.getItem(0).is(Items.DIAMOND));

            // Scrolling (54+) containers: content beyond slot 54 must survive
            // the full-capacity write-back, since the scrolling menu always
            // writes back every backing slot, not just the visible window.
            ItemStack bigBox = new ItemStack(Items.SHULKER_BOX);
            ItemStackContainer big = new ItemStackContainer(bigBox, 243);
            big.setItem(0, new ItemStack(Items.DIAMOND));
            big.setItem(242, new ItemStack(Items.NETHERITE_INGOT));
            ItemStackContainer bigReopened = new ItemStackContainer(bigBox, 243);
            expect("scroll: slot 0 survives round-trip", bigReopened.getItem(0).is(Items.DIAMOND));
            expect("scroll: slot 242 survives round-trip", bigReopened.getItem(242).is(Items.NETHERITE_INGOT));
            expect("scroll: middle slot untouched", bigReopened.getItem(100).isEmpty());

            // container items must be refused on the box slots
            expect("safety: shulker box is a container item", ItemStackContainer.isContainerItem(new ItemStack(Items.SHULKER_BOX)));
            expect("safety: apple is not", !ItemStackContainer.isContainerItem(new ItemStack(Items.APPLE)));

            HandyShulkers.LOGGER.info("[handyshulkers] SELF-TEST PASS: container round-trip OK");
        } catch (Throwable t) {
            HandyShulkers.LOGGER.error("[handyshulkers] SELF-TEST FAIL: {}", t.toString());
        }
    }

    private static boolean nbtHas(ItemStack box, net.minecraft.world.item.Item item) {
        CompoundTag tag = box.getTag();
        if (tag == null || !tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", Tag.TAG_LIST)) {
            return false;
        }
        net.minecraft.nbt.ListTag list = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (ItemStack.of(list.getCompound(i)).is(item)) {
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
