package dev.handyshulkers;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 开服无头自检（对应 1.21 SelfTest，日志含 SELF-TEST）：
 * 1) 盒物品 NBT Items 读/写回环；2) 超出 6 行菜单的隐藏槽在整包写回后原样保留；
 * 3) 套娃守卫谓词。数据完整性回归在无人游玩时也能在日志中暴露。
 */
public final class SelfTest {

    private static int failures;

    private SelfTest() {
    }

    public static void run() {
        try {
            failures = 0;
            ItemStack box = boxWithSlots(2);

            ItemStackContainer container = new ItemStackContainer(box, 27);
            expect("read: diamond visible at slot 0",
                    container.getStackInSlot(0) != null && container.getStackInSlot(0).getItem() == Items.diamond);
            expect("read: apple visible at slot 1",
                    container.getStackInSlot(1) != null && container.getStackInSlot(1).getItem() == Items.apple);
            expect("read: slot 2 empty", container.getStackInSlot(2) == null);

            container.setInventorySlotContents(3, new ItemStack(Items.gold_ingot));
            ItemStackContainer afterWrite = freshContainer(box, 27);
            expect("write: gold ingot persisted into NBT",
                    afterWrite.getStackInSlot(3) != null && afterWrite.getStackInSlot(3).getItem() == Items.gold_ingot);

            ItemStackContainer reopened = freshContainer(box, 27);
            expect("reopen: diamond still at slot 0",
                    reopened.getStackInSlot(0) != null && reopened.getStackInSlot(0).getItem() == Items.diamond);

            reopened.decrStackSize(1, 1);
            expect("remove: apple removed cleanly", freshContainer(box, 27).getStackInSlot(1) == null);

            // 大 NBT：70 个槽的盒装进 54 槽菜单（1.7.10 无滚动界面），隐藏槽必须原样保留
            ItemStack big = boxWithSlots(70);
            ItemStackContainer bigContainer = new ItemStackContainer(big, 54);
            bigContainer.setInventorySlotContents(0, new ItemStack(Items.emerald));
            ItemStackContainer bigReopened = freshContainer(big, 54);
            expect("hidden: slot 0 updated to emerald",
                    bigReopened.getStackInSlot(0) != null && bigReopened.getStackInSlot(0).getItem() == Items.emerald);
            expect("hidden: slot 69 preserved behind the menu",
                    bigReopened.getHidden(15) != null && bigReopened.getHidden(15).getItem() == Items.dye);

            NBTTagList written = big.getTagCompound().getTagList(ItemStackContainer.TAG_ITEMS, 10);
            boolean slot69Present = false;
            for (int i = 0; i < written.tagCount(); i++) {
                if ((written.getCompoundTagAt(i).getByte("Slot") & 255) == 69) {
                    slot69Present = true;
                }
            }
            expect("hidden: write-back keeps all 70 NBT entries", written.tagCount() == 70);
            expect("hidden: slot 69 still in NBT after write-back", slot69Present);

            // 套娃守卫：带 Items NBT 的方块物品禁止入盒；普通物品放行
            ItemStack innerBox = boxWithSlots(0);
            expect("guard: box-like item refused inside a box", ShulkerOpenLogic.isContainerItem(innerBox));
            expect("guard: plain item still allowed", !ShulkerOpenLogic.isContainerItem(new ItemStack(Items.apple)));

            if (failures == 0) {
                HandyShulkers.logger.info("[handyshulkers] SELF-TEST PASS: NBT container round-trip OK");
            } else {
                HandyShulkers.logger.error("[handyshulkers] SELF-TEST FAIL: " + failures + " check(s) failed");
            }
        } catch (Throwable t) {
            HandyShulkers.logger.error("[handyshulkers] SELF-TEST FAIL: " + t);
        }
    }

    /** 带 NBT Items 列表的测试盒：0..count-1 槽各放一枚染色剂。 */
    private static ItemStack boxWithSlots(int count) {
        ItemStack box = new ItemStack(Item.getItemFromBlock(Blocks.chest));
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < count; i++) {
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setByte("Slot", (byte) i);
            new ItemStack(Items.dye, 1, i % 16).writeToNBT(slotTag);
            list.appendTag(slotTag);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(ItemStackContainer.TAG_ITEMS, list);
        box.setTagCompound(tag);
        return box;
    }

    private static ItemStackContainer freshContainer(ItemStack box, int menuSlots) {
        return new ItemStackContainer(box, menuSlots);
    }

    private static void expect(String name, boolean condition) {
        if (!condition) {
            failures++;
            HandyShulkers.logger.error("[handyshulkers] self-test: " + name + " — FAILED");
            throw new IllegalStateException(name);
        }
        HandyShulkers.logger.info("[handyshulkers] self-test: " + name + " — ok");
    }
}
