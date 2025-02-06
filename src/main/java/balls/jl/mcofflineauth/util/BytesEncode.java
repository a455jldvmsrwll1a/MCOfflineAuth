package balls.jl.mcofflineauth.util;

import java.util.Base64;

public class BytesEncode {
    /// Encode bytes as URL-safe base 64 but without trailing '='.
    public static String encode(byte[] buf) {
        String encoded = Base64.getUrlEncoder().encodeToString(buf);
        return encoded.substring(0, encoded.indexOf('='));
    }

    /// Decodes base 64.
    public static byte[] decode(String encoded) throws IllegalArgumentException {
        return Base64.getUrlDecoder().decode(encoded);
    }
}
