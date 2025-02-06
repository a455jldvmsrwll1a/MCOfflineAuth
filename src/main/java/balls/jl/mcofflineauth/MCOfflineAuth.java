package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import balls.jl.mcofflineauth.util.AuthorisedKeysSerialise;
import balls.jl.mcofflineauth.util.KeyEncode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.security.PublicKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static balls.jl.mcofflineauth.Constants.KEYS_PATH;
import static balls.jl.mcofflineauth.Constants.MOD_DIR;

public class MCOfflineAuth implements ModInitializer {
    class ChallengeState {
        static final int CHALLENGE_TIMEOUT = 5;

        private final Instant expiration;
        public final byte[] data;

        public ChallengeState(byte[] data) {
            expiration = Instant.now().plusSeconds(CHALLENGE_TIMEOUT);
            this.data = data;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static boolean AUTH_ACTIVE = true;
    public static HashMap<String, PublicKey> AUTHORISED_KEYS = new HashMap<>();

    private static final HashMap<UUID, ChallengeState> CHALLENGES = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising MCOfflineAuth::Server. (on Fabric)");

        registerPacketPayloads();
        registerEventCallbacks();

        readAuthorisedKeys();
    }

    private static void registerPacketPayloads() {
        PayloadTypeRegistry.configurationS2C().register(LoginChallengePayload.ID, LoginChallengePayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(LoginResponsePayload.ID, LoginResponsePayload.CODEC);

        PayloadTypeRegistry.playS2C().register(PubkeyQueryPayload.ID, PubkeyQueryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PubkeyBindPayload.ID, PubkeyBindPayload.CODEC);
    }

    private static void registerEventCallbacks() {
        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register(MCOfflineAuth::onPreConfigure);
        ServerConfigurationNetworking.registerGlobalReceiver(LoginResponsePayload.ID, MCOfflineAuth::onReceivedChallengeResponse);
        ServerPlayConnectionEvents.JOIN.register(MCOfflineAuth::onPlayerJoin);
        ServerPlayNetworking.registerGlobalReceiver(PubkeyBindPayload.ID, MCOfflineAuth::onReceivedClientBind);
    }

    private static void onPreConfigure(ServerConfigurationNetworkHandler handler, MinecraftServer server) {
        LOGGER.info("debug: onPreConfigure");
    }

    private static void onReceivedChallengeResponse(LoginResponsePayload payload, ServerConfigurationNetworking.Context context) {
        LOGGER.info("debug: onReceivedChallengeResponse");
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        LOGGER.info("debug: onPlayerJoin");
    }

    private static void onReceivedClientBind(PubkeyBindPayload payload, ServerPlayNetworking.Context context) {
        LOGGER.info("debug: onReceivedClientBind");
    }

    /**
     * Read in the authorised keys list from disk.
     * */
    public static void readAuthorisedKeys() {
        try {
            String jsonStr = Files.readString(KEYS_PATH);
            JsonArray pairs = JsonHelper.deserializeArray(jsonStr);
            pairs.forEach(pair -> {
                JsonObject tuple = pair.getAsJsonObject();
                String user = tuple.get("user").getAsString();
                String key = tuple.get("key").getAsString();

                // insertUserKey()
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read authorised-keys.json file: {}", e.toString());
        }

        LOGGER.info("Loaded {} user-key pairs.", AUTHORISED_KEYS.size());
    }

    /**
     * Write the authorised keys list to disk.
     * */
    public static void writeAuthorisedKeys() {
        try {
            Files.createDirectories(MOD_DIR);
            Files.writeString(KEYS_PATH, AuthorisedKeysSerialise.serialiseMap(AUTHORISED_KEYS));
            LOGGER.info("Wrote {} user-key pairs to disk.", AUTHORISED_KEYS.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Binds the user to the specified key.
     *
     * @param user          the username to bind the key to.
     * @param encodedKey    the public key to bind, encoded as a string.
     * @param announce      should this modification be logged?
     * @return              true if an old key was replaced, false if no key was present prior.
     * */
    public static boolean bindEncodedUserKey(String user, String encodedKey, boolean announce) {
        PublicKey key = KeyEncode.decodePublic(encodedKey);
        boolean replaced = AUTHORISED_KEYS.put(user, key) != null;

        if (announce) {
            if (replaced)
                LOGGER.info("User {} was assigned a new key.", user);
            else
                LOGGER.info("User {} was added to the key-pair listing.", user);
        }

        return replaced;
    }

    /**
     * Unbinds the specified user.
     *
     * @param user          the username to unbind.
     * @param announce      should this modification be logged?
     * @return              true if there was such a user bound, false if there is none.
     * */
    public static boolean unbindUserKey(String user, boolean announce) {
        if (!AUTHORISED_KEYS.containsKey(user))
            return false;
        AUTHORISED_KEYS.remove(user);

        if (announce)
                LOGGER.info("User {} was removed from the key-pair listing.", user);

        return true;
    }
}
