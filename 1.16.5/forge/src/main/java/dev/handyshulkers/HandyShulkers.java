package dev.handyshulkers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HandyShulkers {

    public static final String MOD_ID = "handyshulkers";
    public static final String MOD_NAME = "Handy Shulkers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    /**
     * Items that can be opened from the hand. Ships with the 17 vanilla shulker
     * boxes and optionally pulls in conventional {@code #c:shulker_boxes} /
     * {@code #minecraft:shulker_boxes} entries (both optional and absent in
     * 1.16.5, so modded boxes can join the same tag forward-compatibly).
     * Players can extend the tag further with a data pack, and any mod that
     * tags its own box is supported automatically — the 1.21.x extension
     * model, kept intact.
     */
    public static final ITag.INamedTag<Item> SHULKER_BOXES = tag("shulker_boxes");

    /** Items whose hand-use opens a crafting table screen. */
    public static final ITag.INamedTag<Item> CRAFTING_TABLES = tag("crafting_tables");

    /** Items whose hand-use opens a loom screen. */
    public static final ITag.INamedTag<Item> LOOMS = tag("looms");

    /** Items whose hand-use puts the player to sleep right where they stand. */
    public static final ITag.INamedTag<Item> BEDS = tag("beds");

    /** Items whose hand-use opens an enchanting screen (bookshelf bonus kept). */
    public static final ITag.INamedTag<Item> ENCHANTING_TABLES = tag("enchanting_tables");

    /** Items whose hand-use opens a stonecutter screen. */
    public static final ITag.INamedTag<Item> STONECUTTERS = tag("stonecutters");

    /** Items whose hand-use opens a cartography screen. */
    public static final ITag.INamedTag<Item> CARTOGRAPHY_TABLES = tag("cartography_tables");

    /** Items whose hand-use opens a grindstone screen. */
    public static final ITag.INamedTag<Item> GRINDSTONES = tag("grindstones");

    /** Items whose hand-use opens a smithing screen. */
    public static final ITag.INamedTag<Item> SMITHING_TABLES = tag("smithing_tables");

    /** Items whose hand-use opens an anvil screen (no anvil durability used). */
    public static final ITag.INamedTag<Item> ANVILS = tag("anvils");

    /** Items whose hand-use opens the ender chest. */
    public static final ITag.INamedTag<Item> ENDER_CHESTS = tag("ender_chests");

    private static ITag.INamedTag<Item> tag(String name) {
        return ItemTags.bind(new ResourceLocation(MOD_ID, name).toString());
    }

    /** Tag membership helper; the 1.16.5 tag API has no ItemStack overload. */
    public static boolean isIn(ItemStack stack, ITag.INamedTag<Item> tag) {
        return stack.getItem().is(tag);
    }

    private HandyShulkers() {
    }
}
