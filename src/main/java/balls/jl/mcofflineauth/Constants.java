package balls.jl.mcofflineauth;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public static final String MOD_ID = "mc-offline-auth";

    public static final String PERMISSION_STR = MOD_ID;

    public static final int PUBKEY_SIZE = 44;
    public static final String ALGORITHM = "Ed25519";

    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
    public static final Path MOD_DIR = Paths.get(GAME_DIR.toString(), ".offline-auth");
    public static final Path SERVER_CFG_PATH = Paths.get(MOD_DIR.toString(), "server.conf");
    public static final Path KEYS_PATH = Paths.get(MOD_DIR.toString(), "authorised-keys.json");
    public static final Path IGNORED_USERS_PATH = Paths.get(MOD_DIR.toString(), "ignored-users.json");
    public static final Path SEC_PATH = Paths.get(MOD_DIR.toString(), "secret-key");
    public static final Path PUB_PATH = Paths.get(MOD_DIR.toString(), "public-key");

    public static final Identifier PUBKEY_QUERY_PACKET_ID = Identifier.of("mc-offline-auth", "pubkey-query");
    public static final Identifier PUBKEY_BIND_PACKET_ID = Identifier.of("mc-offline-auth", "pubkey-bind");
    public static final Identifier LOGIN_CHALLENGE_PACKET_ID = Identifier.of("mc-offline-auth", "login-challenge");
    public static final Identifier LOGIN_RESPONSE_PACKET_ID = Identifier.of("mc-offline-auth", "login-response");

    public static final Identifier LOGIN_TASK = Identifier.of("mc-offline-auth", "login_task");
}
