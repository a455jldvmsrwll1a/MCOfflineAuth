package balls.jl.mcofflineauth.util;

import balls.jl.mcofflineauth.Constants;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeyEncode {
    /// Encode public key as URL-safe base 64 but without trailing '='.
    public static String encodePublic(PublicKey key) {
        return BytesEncode.encode(key.getEncoded());
    }

    /// Decodes a base 64 string as a public key.
    public static PublicKey decodePublic(String encoded) throws IllegalArgumentException {
        try {
            byte[] keyBytes = BytesEncode.decode(encoded);
            KeyFactory kf = KeyFactory.getInstance(Constants.ALGORITHM);
            return kf.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException(e.getCause());
        }
    }

    /// Decodes encoded bytes as a public key.
    public static PublicKey decodePublic(byte[] encoded) throws IllegalArgumentException {
        try {
            KeyFactory kf = KeyFactory.getInstance(Constants.ALGORITHM);
            return kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException(e.getCause());
        }
    }
}
