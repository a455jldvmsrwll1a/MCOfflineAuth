package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.util.IgnoredUsersSerialise;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static balls.jl.mcofflineauth.Constants.IGNORED_USERS_PATH;
import static balls.jl.mcofflineauth.Constants.MOD_DIR;

public class IgnoredUsers {
    private static final Set<String> IGNORED_USERNAMES = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> IGNORED_UUIDS = ConcurrentHashMap.newKeySet();
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static Set<String> getIgnoredUsernames() {
        return IGNORED_USERNAMES;
    }

    public static Set<UUID> getIgnoredUuids() {
        return IGNORED_UUIDS;
    }

    /**
     * Read in the authorised keys list from disk.
     */
    public static void read() {
        IGNORED_USERNAMES.clear();
        IGNORED_UUIDS.clear();

        try {
            String jsonStr = Files.readString(IGNORED_USERS_PATH);
            JsonArray entries = JsonHelper.deserializeArray(jsonStr);
            entries.forEach(entry -> {
                JsonObject key = entry.getAsJsonObject();

                if (key.has("uuid")) {
                    String uuidString = key.get("uuid").getAsString();
                    IGNORED_UUIDS.add(UUID.fromString(uuidString));
                } else if (key.has("name")) {
                    IGNORED_USERNAMES.add(key.get("name").getAsString());
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read ignored-users.json file: {}", e.toString());
        }

        LOGGER.info("Loaded {} ignored usernames, {} ignored UUIDs.", IGNORED_USERNAMES.size(), IGNORED_UUIDS.size());
    }

    /**
     * Write the authorised keys list to disk.
     */
    public static void write() {
        try {
            Files.createDirectories(MOD_DIR);
            Files.writeString(IGNORED_USERS_PATH, IgnoredUsersSerialise.serialiseIgnored(IGNORED_USERNAMES, IGNORED_UUIDS));
            LOGGER.info("Wrote {} ignored names, {} ignored uuids to disk.", IGNORED_USERNAMES.size(), IGNORED_UUIDS.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add username to ignore list, and write to disk.
     *
     * @return true if not already ignored.
     */
    public static boolean ignoreUsername(String username) {
        boolean changed = IGNORED_USERNAMES.add(username);
        if (changed)
            write();
        return changed;
    }

    /**
     * Add player UUID to ignore list.
     *
     * @return true if not already ignored.
     */
    public static boolean ignoreUUID(UUID player) {
        boolean changed = IGNORED_UUIDS.add(player);
        if (changed)
            write();
        return changed;
    }

    /**
     * Remove username from ignore list.
     *
     * @return true if not already removed.
     */
    public static boolean unignoreUsername(String username) {
        boolean changed = IGNORED_USERNAMES.remove(username);
        if (changed)
            write();
        return changed;
    }

    /**
     * Remove UUID from ignore list.
     *
     * @return true if not already removed.
     */
    public static boolean unignoreUUID(UUID player) {
        boolean changed = IGNORED_UUIDS.remove(player);
        if (changed)
            write();
        return changed;
    }

    /**
     * Add UUID to ignore list.
     *
     * @return true if the player should be exempt from this mod.
     */
    public static boolean playerIsIgnored(GameProfile player) {
        return IGNORED_UUIDS.contains(player.getId()) || IGNORED_USERNAMES.contains(player.getName());
    }
}
