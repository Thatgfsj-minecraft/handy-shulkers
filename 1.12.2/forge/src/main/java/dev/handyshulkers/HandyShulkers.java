package dev.handyshulkers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared constants. 1.12.2 has no item tags (they arrive in 1.13), so the
 * 1.21.x "join a tag to get supported" model collapses into plain item-class
 * checks plus config whitelists; see {@link ShulkerOpenLogic} and
 * {@link HandyShulkersConfig} for the replacement.
 */
public final class HandyShulkers {

    public static final String MOD_ID = "handyshulkers";
    public static final String MOD_NAME = "Handy Shulkers";
    public static final String VERSION = "1.4.1";

    public static Logger LOGGER = LogManager.getLogger(MOD_ID);

    private HandyShulkers() {
    }
}
