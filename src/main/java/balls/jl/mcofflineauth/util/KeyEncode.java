package balls.jl.mcofflineauth.util;

import balls.jl.mcofflineauth.Constants;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyEncode {
    /// Encode public key as URL-safe base 64 but without trailing '='.
    public static String encodePublic(PublicKey key) {
        return BytesEncode.encode(key.getEncoded());
    }

    /// Decodes base 64 as a public key.
    public static PublicKey decodePublic(String encoded) throws IllegalArgumentException {
        try {
            byte[] keyBytes = BytesEncode.decode(encoded);
            KeyFactory kf = KeyFactory.getInstance(Constants.ALGORITHM);
            PublicKey key = kf.generatePublic(new X509EncodedKeySpec(keyBytes));
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException(e.getCause());
        }
    }
}
