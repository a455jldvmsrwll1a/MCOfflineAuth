package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.util.ConfigSerialise;
import balls.jl.mcofflineauth.util.DefaultMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);
    private static final ConcurrentHashMap<String, String> MESSAGES = new ConcurrentHashMap<>();
    private volatile static boolean AUTH_ENFORCING = true;
    private volatile static boolean KEEP_ENCRYPTION = false;
    private volatile static boolean ALLOW_UNBOUND_USERS = true;
    private volatile static boolean PREVENT_LOGIN_KICK = true;
    private volatile static boolean PREVENT_LOGIN_KICK_UNBOUND = false;
    private volatile static boolean WARN_UNAUTHORISED_LOGINS = true;
    private volatile static boolean CHANGES_REQUIRE_APPROVAL = false;
    private volatile static int UNBOUND_USER_GRACE_PERIOD = 300;

    public static boolean isEnforcing() {
        return AUTH_ENFORCING;
    }

    public static boolean keepingEncryption() {
        return KEEP_ENCRYPTION;
    }

    public static boolean allowsUnboundUsers() {
        return ALLOW_UNBOUND_USERS;
    }

    public static boolean preventsLoginKick() {
        return PREVENT_LOGIN_KICK;
    }

    public static boolean preventsLoginKickUnbound() {
        return PREVENT_LOGIN_KICK_UNBOUND;
    }

    public static boolean warnsUnauthorisedLogins() {
        return WARN_UNAUTHORISED_LOGINS;
    }

    public static boolean changesRequireApproval() {
        return CHANGES_REQUIRE_APPROVAL;
    }

    public static int getUnboundUserGracePeriod() {
        return UNBOUND_USER_GRACE_PERIOD;
    }

    public static String message(String id) {
        return MESSAGES.getOrDefault(id, id);
    }

    public static ConcurrentHashMap<String, String> messages() {
        return MESSAGES;
    }

    public static void print(String id, Consumer<String> callback) {
        String message = MESSAGES.get(id);
        if (message != null && !message.isBlank()) callback.accept(message);
    }

    public static boolean setEnforcing(boolean enforce) {
        if (AUTH_ENFORCING == enforce) return false;

        AUTH_ENFORCING = enforce;
        return true;
    }

    public static void shouldKeepEncryption(boolean keep) {
        KEEP_ENCRYPTION = keep;
    }

    public static void setAllowUnboundUsers(boolean allow) {
        ALLOW_UNBOUND_USERS = allow;
    }

    public static void setPreventLoginKick(boolean prevent) {
        PREVENT_LOGIN_KICK = prevent;
    }

    public static void setPreventLoginKickUnbound(boolean prevent) {
        PREVENT_LOGIN_KICK_UNBOUND = prevent;
    }

    public static void setWarnUnauthorisedLogins(boolean warn) {
        WARN_UNAUTHORISED_LOGINS = warn;
    }

    public static void shouldChangesRequireApproval(boolean require) {
        CHANGES_REQUIRE_APPROVAL = true;
    }

    public static void setMessage(String id, String message) {
        MESSAGES.put(id, message);
    }

    public static void clearMessage(String id) {
        MESSAGES.remove(id);
    }

    public static void setUnboundUserGracePeriod(int seconds) {
        UNBOUND_USER_GRACE_PERIOD = seconds;
    }

    public static void read() {
        try {
            MESSAGES.clear();
            DefaultMessages.setDefaultMessages();
            ConfigSerialise.deserialise(Files.readString(Constants.SERVER_CFG_PATH));
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
