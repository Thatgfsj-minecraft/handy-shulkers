package dev.handyshulkers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HandyShulkers {

    public static final String MOD_ID = "handyshulkers";
    public static final String MOD_NAME = "Handy Shulkers";

    /**
     * Items that can be opened from the hand. Ships with the 17 vanilla shulker
     * boxes and optionally pulls in {@code #c:shulker_boxes} and
     * {@code #minecraft:shulker_boxes}, so any modded box that joins either
     * conventional tag is supported automatically. Players can extend the tag
     * further with a resource pack.
     */
    public static final TagKey<Item> SHULKER_BOXES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "shulker_boxes"));

    /** Items whose hand-use opens a crafting table screen. */
    public static final TagKey<Item> CRAFTING_TABLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "crafting_tables"));

    /** Items whose hand-use opens a loom screen. */
    public static final TagKey<Item> LOOMS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "looms"));

    /** Items whose hand-use puts the player to sleep right where they stand. */
    public static final TagKey<Item> BEDS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "beds"));

    private HandyShulkers() {
    }
}
