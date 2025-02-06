package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.util.AuthorisedKeysSerialise;
import balls.jl.mcofflineauth.util.KeyEncode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.util.HashMap;

import static balls.jl.mcofflineauth.Constants.*;

public class AuthorisedKeys {
    public static final HashMap<String, PublicKey> KEYS = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    /**
     * Number of entries in the list.
     */
    public static int count() {
        return KEYS.size();
    }

    /**
     * Read in the authorised keys list from disk.
     */
    public static void read() {
        KEYS.clear();

        try {
            String jsonStr = Files.readString(KEYS_PATH);
            JsonArray pairs = JsonHelper.deserializeArray(jsonStr);
            pairs.forEach(pair -> {
                JsonObject tuple = pair.getAsJsonObject();
                String user = tuple.get("user").getAsString();
                String key = tuple.get("key").getAsString();

                insertUser(user, key);
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read authorised-keys.json file: {}", e.toString());
        }

        LOGGER.info("Loaded {} user-key pairs.", KEYS.size());
    }

    /**
     * Write the authorised keys list to disk.
     */
    public static void write() {
        try {
            Files.createDirectories(MOD_DIR);
            Files.writeString(KEYS_PATH, AuthorisedKeysSerialise.serialiseMap(KEYS));
            LOGGER.info("Wrote {} user-key pairs to disk.", KEYS.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Insert the user in the list.
     *
     * @param user       the username to bind the key to.
     * @param encodedKey the public key to bind, encoded as a string.
     * @return true if an old key was replaced, false if no key was present prior.
     */
    public static boolean insertUser(String user, String encodedKey) {
        return KEYS.put(user, KeyEncode.decodePublic(encodedKey)) != null;
    }

    /**
     * Binds the user to the specified key.
     *
     * @param user       the username to bind the key to.
     * @param encodedKey the public key to bind, encoded as a string.
     * @param announce   should this modification be logged?
     * @return true if an old key was replaced, false if no key was present prior.
     */
    public static boolean bind(String user, String encodedKey, boolean announce) {
        boolean replaced = insertUser(user, encodedKey);

        if (announce) {
            if (replaced) LOGGER.info("User {} was assigned a new key.", user);
            else LOGGER.info("User {} was added to the key-pair listing.", user);
        }

        write();
        return replaced;
    }

    /**
     * Unbinds the specified user.
     *
     * @param user     the username to unbind.
     * @param announce should this modification be logged?
     * @return true if there was such a user bound, false if there is none.
     */
    public static boolean unbind(String user, boolean announce) {
        if (!KEYS.containsKey(user)) return false;
        KEYS.remove(user);

        if (announce) LOGGER.info("User {} was removed from the key-pair listing.", user);

        write();
        return true;
    }

    /**
     * Unbinds ALL known users.
     */
    public static boolean clear(boolean announce) {
        KEYS.clear();

        if (announce) LOGGER.info("ALL users were removed!");

        write();
        return true;
    }

    /**
     * Attempt to verify the signature against the given data and user's key.
     *
     * @param user      the user whose key will be used.
     * @param data      the data to verify the signature for.
     * @param signature the signature to verify.
     * @return true if valid, false if not.
     */
    public static boolean verifySignature(String user, byte[] data, byte[] signature) {
        try {
            PublicKey publicKey = KEYS.get(user);
            if (publicKey == null)
                return false;

            Signature sig = Signature.getInstance(ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);

            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }
}
