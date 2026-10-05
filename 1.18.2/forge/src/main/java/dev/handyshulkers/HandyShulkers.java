package dev.handyshulkers;

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
     * boxes and optionally pulls in {@code #c:shulker_boxes}, so any modded box
     * that joins the conventional tag is supported automatically. Players can
     * extend the tag further with a resource pack.
     */
    public static final TagKey<Item> SHULKER_BOXES = tag("shulker_boxes");

    /** Items whose hand-use opens a crafting table screen. */
    public static final TagKey<Item> CRAFTING_TABLES = tag("crafting_tables");

    /** Items whose hand-use opens a loom screen. */
    public static final TagKey<Item> LOOMS = tag("looms");

    /** Items whose hand-use puts the player to sleep right where they stand. */
    public static final TagKey<Item> BEDS = tag("beds");

    /** Items whose hand-use opens an enchanting screen. */
    public static final TagKey<Item> ENCHANTING_TABLES = tag("enchanting_tables");

    /** Items whose hand-use opens a stonecutter screen. */
    public static final TagKey<Item> STONECUTTERS = tag("stonecutters");

    /** Items whose hand-use opens a cartography screen. */
    public static final TagKey<Item> CARTOGRAPHY_TABLES = tag("cartography_tables");

    /** Items whose hand-use opens a grindstone screen. */
    public static final TagKey<Item> GRINDSTONES = tag("grindstones");

    /** Items whose hand-use opens a smithing screen. */
    public static final TagKey<Item> SMITHING_TABLES = tag("smithing_tables");

    /** Items whose hand-use opens an anvil screen (no anvil durability used). */
    public static final TagKey<Item> ANVILS = tag("anvils");

    /** Items whose hand-use opens the ender chest. */
    public static final TagKey<Item> ENDER_CHESTS = tag("ender_chests");

    private static TagKey<Item> tag(String name) {
        return TagKey.create(RegistryHelper.itemRegistryKey(), new ResourceLocation(MOD_ID, name));
    }

    /**
     * 1.18.2 shim: {@code TagKey.create} takes the registry's {@code ResourceKey},
     * {@code Registry.ITEM_REGISTRY}; kept behind one call site so the API
     * difference to newer versions stays in one place.
     */
    private static final class RegistryHelper {
        static net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Item>> itemRegistryKey() {
            return net.minecraft.core.Registry.ITEM_REGISTRY;
        }
    }

    private HandyShulkers() {
    }
}
