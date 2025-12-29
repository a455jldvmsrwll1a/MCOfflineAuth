package balls.jl.mcofflineauth.util;

import balls.jl.mcofflineauth.Constants;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeyEncode {
    // ASN.1 DER header in base64.
    private final static String PREFIX = "MCowBQYDK2VwAyEA";
    private final static int PREFIX_LENGTH = PREFIX.length();
    private final static int FULL_LENGTH = 59;
    private final static int SHORT_LENGTH = FULL_LENGTH - PREFIX_LENGTH;

    /// Encode public key as URL-safe base 64 but without trailing '='.
    public static String encodePublic(PublicKey key) {
        return BytesEncode.encode(key.getEncoded()).substring(PREFIX_LENGTH);
    }

    /// Decodes a base 64 string as a public key.
    public static PublicKey decodePublic(String encoded) throws IllegalArgumentException {
        if (encoded.length() != FULL_LENGTH) {
            if (encoded.length() != SHORT_LENGTH) {
                throw new IllegalArgumentException("Not a valid full or shortened public key string.");
            }

            // Make into full string.
            encoded = PREFIX + encoded;
        }

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
