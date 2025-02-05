package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
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


    private static final HashMap<UUID, ChallengeState> CHALLENGES = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising MCOfflineAuth::Server. (on Fabric)");

        registerPacketPayloads();
        registerEventCallbacks();
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
}
