package balls.jl.mcofflineauth;

import static balls.jl.mcofflineauth.Constants.MOD_DIR;
import static balls.jl.mcofflineauth.Constants.UUID_REMAPS_PATH;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UUIDRemap {
    public static final ConcurrentHashMap<UUID, UUID> REMAPS = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    /**
     * Number of entries in the list.
     */
    public static int count() {
        return REMAPS.size();
    }

    /**
     * Read in the UUID remaps from disk.
     */
    public static void read() {
        REMAPS.clear();
        int line_num = 0;

        try {
            String str = Files.readString(UUID_REMAPS_PATH);
            String[] lines = str.lines().toArray(String[]::new);

            for (String line : lines) {
                line_num++;

                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] words = line.split("\\s+");
                LOGGER.info("DEBUG: words are: {}", (Object) words);
                if (words.length != 2) {
                    throw new Exception("%d tokens found but 2 was expected!".formatted(words.length));
                }

                UUID src = UUID.fromString(words[0]);
                UUID dest = UUID.fromString(words[1]);

                REMAPS.put(src, dest);
            }

        } catch (Exception e) {
            LOGGER.warn("Could not read uuid-remaps.conf file: at line {}: {}", line_num, e.toString());
        }

        LOGGER.info("Loaded {} UUID remaps.", REMAPS.size());
    }

    /**
     * Write the UUID remaps to disk.
     */
    public static void write() {
        try {
            Files.createDirectories(MOD_DIR);
            StringBuilder sb = new StringBuilder();
            sb.append("# <old uuid> <new uuid>\n");
            REMAPS.forEach((src, dest) -> {
                sb.append(src);
                sb.append(' ');
                sb.append(dest);
                sb.append('\n');
            });

            sb.append('\n');

            Files.writeString(UUID_REMAPS_PATH, sb);
            LOGGER.info("Wrote {} UUID remaps to disk.", REMAPS.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Map a UUID into another UUID.
     *
     * @param src the UUID to be replaced.
     * @param dest the UUID to replace with.
     * @param announce should this modification be logged?
     */
    public static void map(UUID src, UUID dest, boolean announce) {
        if (announce) LOGGER.info("UUID Remap {} --> {} was added to the list.", src, dest);

        if (REMAPS.get(src) == dest) {
            return;
        }

        REMAPS.put(src, dest);

        write();
    }

    /**
     * Remove a UUID remap.
     *
     * @param source     the source UUID of the remap.
     * @param announce should this modification be logged?
     * @return true if there was such a remap, false if there is none.
     */
    public static boolean unmap(UUID source, boolean announce) {
        if (!REMAPS.containsKey(source)) return false;
        UUID old_dest = REMAPS.remove(source);

        if (announce) LOGGER.info("UUID Remap {} --> {} was removed from the list.", source, old_dest);

        write();
        return true;
    }
}
