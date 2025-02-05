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

public class MCOfflineAuth implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);



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
