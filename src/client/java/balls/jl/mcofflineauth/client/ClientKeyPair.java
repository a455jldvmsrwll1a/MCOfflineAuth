package balls.jl.mcofflineauth.client;

import balls.jl.mcofflineauth.util.KeyEncode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import static balls.jl.mcofflineauth.Constants.*;

public class ClientKeyPair {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyPair KEY_PAIR;

    public static void loadOrCreate() {
        try {
            load();
        } catch (RuntimeException e) {
            generate();
        }
    }

    public static void load() {
        PrivateKey secretKey;
        PublicKey publicKey;

        try {
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            secretKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(SEC_PATH)));
        } catch (IOException e) {
            LOGGER.warn("Secret key file could not be loaded: {}", e.toString());
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Loaded invalid private key: {}", e.toString());
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            publicKey = KeyEncode.decodePublic(Files.readAllBytes(PUB_PATH));
        } catch (IOException e) {
            LOGGER.warn("Public key file could not be loaded: {}", e.toString());
            throw new RuntimeException(e);
        }

        KEY_PAIR = new KeyPair(publicKey, secretKey);
        LOGGER.info("Successfully loaded key-pair from disk.");
    }

    public static void generate() {
        try {
            LOGGER.info("Generating new {} key-pair.", ALGORITHM);

            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            KEY_PAIR = generator.generateKeyPair();

            Files.write(SEC_PATH, KEY_PAIR.getPrivate().getEncoded());
            Files.write(PUB_PATH, KEY_PAIR.getPublic().getEncoded());

            LOGGER.info("Successfully wrote key-pair to disk.");
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
