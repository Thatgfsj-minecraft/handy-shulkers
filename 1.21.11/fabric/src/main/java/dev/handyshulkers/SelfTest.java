package dev.handyshulkers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

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
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));

            ItemStackContainer container = new ItemStackContainer(box, 27);
            expect("read: diamond visible at slot 0", container.getItem(0).is(Items.DIAMOND));
            expect("read: slot 1 empty", container.getItem(1).isEmpty());

            container.setItem(1, new ItemStack(Items.APPLE));
            expect("write: apple persisted into component", componentHas(box, Items.APPLE));

            ItemStackContainer reopened = new ItemStackContainer(box, 27);
            expect("reopen: apple visible at slot 1", reopened.getItem(1).is(Items.APPLE));
            expect("reopen: diamond still at slot 0", reopened.getItem(0).is(Items.DIAMOND));

            reopened.removeItem(1, 1);
            ItemStackContainer afterRemoval = new ItemStackContainer(box, 27);
            expect("remove: apple removed cleanly", afterRemoval.getItem(1).isEmpty());

            HandyShulkers.LOGGER.info("[handyshulkers] SELF-TEST PASS: container round-trip OK");
        } catch (Throwable t) {
            HandyShulkers.LOGGER.error("[handyshulkers] SELF-TEST FAIL: {}", t.toString());
        }
    }

    private static boolean componentHas(ItemStack box, net.minecraft.world.item.Item item) {
        ItemContainerContents contents = box.get(DataComponents.CONTAINER);
        return contents != null && contents.stream().anyMatch(s -> s.is(item));
    }

    private static void expect(String name, boolean condition) {
        if (!condition) {
            throw new IllegalStateException(name);
        }
        HandyShulkers.LOGGER.info("[handyshulkers] self-test: {} — ok", name);
    }
}
