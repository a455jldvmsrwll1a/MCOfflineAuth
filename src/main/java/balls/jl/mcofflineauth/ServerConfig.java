package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.util.ConfigSerialise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

public class ServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private static boolean AUTH_ENFORCING = true;
    private static boolean ALLOW_UNBOUND_USERS = true;

    public static boolean isEnforcing() {
        return AUTH_ENFORCING;
    }

    public static boolean allowsUnboundUsers() {
        return ALLOW_UNBOUND_USERS;
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

    public static void read() {
        try {
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
