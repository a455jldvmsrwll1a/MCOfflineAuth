package balls.jl.mcofflineauth.util;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthorisedKeysSerialise {
    /**
     * Serialise a hash map of authorised keys as a JSON string.
     * */
    public static String serialiseMap(HashMap<String, PublicKey> keys) {
        if (keys.isEmpty())
            return "[]\n";

        ArrayList<Map.Entry<String, PublicKey>> arr = new ArrayList<>(keys.entrySet());
        int length = arr.size();

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < length; ++i) {
            sb.append("    {\n");
            sb.append("        \"user\": \"%s\",\n".formatted(arr.get(i).getKey()));
            sb.append("        \"key\": \"%s\"\n".formatted(KeyEncode.encodePublic(arr.get(i).getValue())));
            sb.append("    }");
            if (i != length - 1)
                sb.append(',');
            sb.append('\n');
        }

        sb.append("]\n");
        return sb.toString();
    }
}
