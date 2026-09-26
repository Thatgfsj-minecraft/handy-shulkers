package dev.handyshulkers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "shulker_boxes"));

    private HandyShulkers() {
    }
}
