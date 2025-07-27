package balls.jl.mcofflineauth.util;

import java.util.*;

public class IgnoredUsersSerialise {
    /**
     * Serialise ignore entries as a JSON string.
     */
    public static String serialiseIgnored(Set<String> usernames, Set<UUID> uuids) {
        if (usernames.isEmpty() && uuids.isEmpty())
            return "[]\n";

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        Iterator<String> iter_usernames = usernames.iterator();
        Iterator<UUID> iter_uuids = uuids.iterator();

        while (iter_usernames.hasNext()) {
            String name = iter_usernames.next();
            sb.append("    { \"name\": \"%s\" }".formatted(name));
            // Make sure we put a comma after the usernames for the UUIDs following.
            if (iter_usernames.hasNext() || iter_uuids.hasNext())
                sb.append(',');
            sb.append('\n');
        }

        while (iter_uuids.hasNext()) {
            UUID uuid = iter_uuids.next();
            sb.append("    { \"uuid\": \"%s\" }".formatted(uuid));
            if (iter_uuids.hasNext())
                sb.append(',');
            sb.append('\n');
        }

        sb.append("]\n");
        return sb.toString();
    }
}
