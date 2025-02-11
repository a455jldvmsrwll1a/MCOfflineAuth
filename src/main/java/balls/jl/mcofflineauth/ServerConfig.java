package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.util.ConfigSerialise;
import balls.jl.mcofflineauth.util.DefaultMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Consumer;

public class ServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private static boolean AUTH_ENFORCING = true;
    private static boolean ALLOW_UNBOUND_USERS = true;
    private static final HashMap<String, String> MESSAGES = new HashMap<>();

    public static boolean isEnforcing() {
        return AUTH_ENFORCING;
    }

    public static boolean allowsUnboundUsers() {
        return ALLOW_UNBOUND_USERS;
    }

    public static String message(String id) {
        return MESSAGES.getOrDefault(id, id);
    }

    public static HashMap<String, String> messages() {
        return MESSAGES;
    }

    public static void print(String id, Consumer<String> callback) {
        String message = MESSAGES.get(id);
        if (message != null && !message.isBlank())
            callback.accept(message);
    }

    public static boolean setEnforcing(boolean enforce) {
        if (AUTH_ENFORCING == enforce)
            return false;

        AUTH_ENFORCING = enforce;
        return true;
    }

    public static boolean setAllowUnboundUsers(boolean allow) {
        if (ALLOW_UNBOUND_USERS == allow)
            return false;

        ALLOW_UNBOUND_USERS = allow;
        return true;
    }

    public static void setMessage(String id, String message) {
        MESSAGES.put(id, message);
    }

    public static void clearMessage(String id) {
        MESSAGES.remove(id);
    }

    public static void read() {
        try {
            MESSAGES.clear();
            DefaultMessages.setDefaultMessages();
            ConfigSerialise.deserialise(Files.readString(Constants.SERVER_CFG_PATH));
            LOGGER.info("debug: messages: {}", MESSAGES);
        } catch (IOException e) {
            LOGGER.warn("Could not read server.conf file: {}", e.toString());
        }

        LOGGER.warn("Loaded server config.");
    }

    public static void write() {
        try {
            Files.createDirectories(Constants.MOD_DIR);
            Files.writeString(Constants.SERVER_CFG_PATH, ConfigSerialise.serialise());
            LOGGER.info("Wrote configuration to disk.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
