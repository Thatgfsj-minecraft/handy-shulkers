package dev.handyshulkers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandyShulkers {

    public static final String MOD_ID = "handyshulkers";
    public static final String MOD_NAME = "Handy Shulkers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

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

    /** Items whose hand-use opens an enchanting screen. */
    public static final TagKey<Item> ENCHANTING_TABLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "enchanting_tables"));

    /** Items whose hand-use opens a stonecutter screen. */
    public static final TagKey<Item> STONECUTTERS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "stonecutters"));

    /** Items whose hand-use opens a cartography screen. */
    public static final TagKey<Item> CARTOGRAPHY_TABLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "cartography_tables"));

    /** Items whose hand-use opens a grindstone screen. */
    public static final TagKey<Item> GRINDSTONES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "grindstones"));

    /** Items whose hand-use opens a smithing screen. */
    public static final TagKey<Item> SMITHING_TABLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "smithing_tables"));

    /** Items whose hand-use opens an anvil screen (no anvil durability used). */
    public static final TagKey<Item> ANVILS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "anvils"));

    /** Items whose hand-use opens the ender chest. */
    public static final TagKey<Item> ENDER_CHESTS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "ender_chests"));

    private HandyShulkers() {
    }
}
