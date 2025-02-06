package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.command.Commands;
import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

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

    private static final HashMap<UUID, ChallengeState> CHALLENGES = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising MCOfflineAuth::Server. (on Fabric)");
        showEscapeOfAccountability();

        registerPacketPayloads();
        registerEventCallbacks();

        AuthorisedKeys.read();
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

        CommandRegistrationCallback.EVENT.register(Commands::register);
    }

    private static void onPreConfigure(ServerConfigurationNetworkHandler handler, MinecraftServer server) {
        LOGGER.info("debug: onPreConfigure");
    }

    private static void onReceivedChallengeResponse(LoginResponsePayload payload, ServerConfigurationNetworking.Context context) {
        LOGGER.info("debug: onReceivedChallengeResponse");
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        if (!server.isSingleplayer() && !AuthorisedKeys.KEYS.containsKey(handler.player.getName().getString())) {
            handler.player.sendMessage(Text.literal("Attention: this username is unclaimed!").formatted(Formatting.RED, Formatting.BOLD));
            handler.player.sendMessage(Text.literal("No key is bound to this username; anyone can join with this name."));
            handler.player.sendMessage(Text.literal("§aClick this text or type \"§f/offauth bind§a\" to bind your key!§r").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click --> /offauth bind"))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/offauth bind"))));
        }
    }

    private static void onReceivedClientBind(PubkeyBindPayload payload, ServerPlayNetworking.Context context) {
        LOGGER.info("debug: onReceivedClientBind");
    }

    private static void showEscapeOfAccountability() {
        LOGGER.warn("DISCLAIMER: This mod is experimental software, it has not undergone");
        LOGGER.warn("extensive testing. I am not responsible for any griefed servers.");
        LOGGER.warn("It is always better to avoid offline mode if possible.");
        LOGGER.warn("USE THIS SOFTWARE AT YOUR OWN RISK.");
    }
}
