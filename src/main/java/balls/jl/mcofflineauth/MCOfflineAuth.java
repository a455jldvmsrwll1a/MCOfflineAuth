package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.command.Commands;
import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import balls.jl.mcofflineauth.util.KeyEncode;
import lol.bai.badpackets.api.PacketReceiver;
import lol.bai.badpackets.api.config.ConfigPackets;
import lol.bai.badpackets.api.config.ConfigTaskExecutor;
import lol.bai.badpackets.api.config.ServerConfigContext;
import lol.bai.badpackets.api.play.PlayPackets;
import lol.bai.badpackets.api.play.ServerPlayContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MCOfflineAuth implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private static final ConcurrentHashMap<UUID, ChallengeState> CHALLENGES = new ConcurrentHashMap<>();

    private static void registerPacketPayloads() {
        ConfigPackets.registerClientChannel(LoginChallengePayload.ID, LoginChallengePayload.CODEC);
        ConfigPackets.registerServerChannel(LoginResponsePayload.ID, LoginResponsePayload.CODEC);

        PlayPackets.registerClientChannel(PubkeyQueryPayload.ID, PubkeyQueryPayload.CODEC);
        PlayPackets.registerServerChannel(PubkeyBindPayload.ID, PubkeyBindPayload.CODEC);
    }

    private static void registerEventCallbacks() {
        ConfigPackets.registerTask(Constants.LOGIN_TASK, new LoginTask());
        ConfigPackets.registerServerReceiver(LoginResponsePayload.ID, new LoginResponseReceiver());
        PlayPackets.registerServerReceiver(PubkeyBindPayload.ID, new PubkeyBindReceiver());

        ServerPlayConnectionEvents.JOIN.register(MCOfflineAuth::onPlayerJoin);

        CommandRegistrationCallback.EVENT.register(Commands::register);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        server.execute(MCOfflineAuth::checkForExpiredChallenges);
        ServerPlayerEntity player = handler.getPlayer();

        if (!server.isSingleplayer() && !AuthorisedKeys.KEYS.containsKey(player.getName().getString())) {
            ServerConfig.print("noKeyBannerHeader", (msg) -> player.sendMessage(Text.of(msg)));
            ServerConfig.print("noKeyBannerInfo", (msg) -> player.sendMessage(Text.of(msg)));
            ServerConfig.print("noKeyBannerHint", (msg) -> player.sendMessage(Text.literal(msg).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click --> /offauth bind"))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/offauth bind")))));
        }
    }

    public static void checkForExpiredChallenges() {
        CHALLENGES.forEach((uuid, state) -> {
            if (state.isExpired()) CHALLENGES.remove(uuid);
        });
    }

    private static LoginChallengePayload spawnChallenge() {
        SecureRandom rng = new SecureRandom();
        UUID uuid = new UUID(rng.nextLong(), rng.nextLong());

        byte[] plainText = new byte[512];
        rng.nextBytes(plainText);

        LoginChallengePayload payload = new LoginChallengePayload(uuid, plainText);
        CHALLENGES.put(uuid, new ChallengeState(plainText));

        return payload;
    }

    private static void showEscapeOfAccountability() {
        LOGGER.warn("DISCLAIMER: This mod is experimental software, it has not undergone");
        LOGGER.warn("extensive testing. I am not responsible for any griefed servers.");
        LOGGER.warn("It is always better to avoid offline mode if possible.");
        LOGGER.warn("USE THIS SOFTWARE AT YOUR OWN RISK.");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising MCOfflineAuth::Server. (on Fabric)");
        showEscapeOfAccountability();

        registerPacketPayloads();
        registerEventCallbacks();

        ServerConfig.read();
        AuthorisedKeys.read();
    }

    static class ChallengeState {
        static final int CHALLENGE_TIMEOUT = 5;
        public final byte[] data;
        private final Instant expiration;

        public ChallengeState(byte[] data) {
            expiration = Instant.now().plusSeconds(CHALLENGE_TIMEOUT);
            this.data = data;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }
    }

    static class LoginTask implements ConfigTaskExecutor {

        @Override
        public boolean runTask(ServerConfigContext context) {
            if (!ServerConfig.isEnforcing()) {
                LOGGER.warn("MCOfflineAuth is on standby; won't do anything here.");
                return false;
            }

            if (!context.canSend(LoginChallengePayload.ID)) {
                context.handler().onDisconnected(new DisconnectionInfo(Text.of("Client does not have MCOfflineAuth installed.")));
                context.handler().disconnect(Text.of(ServerConfig.message("accessDenied")));
                return false;
            }

            LoginChallengePayload payload = spawnChallenge();
            context.send(payload);
            return true;
        }
    }

    static class LoginResponseReceiver implements PacketReceiver<lol.bai.badpackets.api.config.ServerConfigContext, balls.jl.mcofflineauth.net.LoginResponsePayload> {

        @Override
        public void receive(ServerConfigContext context, LoginResponsePayload payload) {
            checkForExpiredChallenges();
            context.finishTask(Constants.LOGIN_TASK);

            ChallengeState state = CHALLENGES.remove(payload.id);
            if (state == null) {
                context.handler().disconnect(Text.of(ServerConfig.message("timeout")));
                return;
            }

            if (!AuthorisedKeys.KEYS.containsKey(payload.user)) {
                LOGGER.warn("Connecting user {} is not in the database.", payload.user);

                // Skip verification for unbound usernames if it's allowed.
                if (ServerConfig.allowsUnboundUsers()) return;

                // Else, kick them.
                context.handler().disconnect(Text.of(ServerConfig.message("kickNoKey")));
                return;
            }

            if (!AuthorisedKeys.verifySignature(payload.user, state.data, payload.signature)) {
                context.handler().disconnect(Text.of(ServerConfig.message("wrongIdentity")));
                return;
            }

            LOGGER.info("Verified {}'s identity successfully.", payload.user);
        }
    }

    static class PubkeyBindReceiver implements PacketReceiver<lol.bai.badpackets.api.play.ServerPlayContext, balls.jl.mcofflineauth.net.PubkeyBindPayload> {

        @Override
        public void receive(ServerPlayContext context, PubkeyBindPayload payload) {
            context.server().execute(() -> {
                checkForExpiredChallenges();

                if (!Objects.equals(payload.user, context.player().getName().getString())) {
                    LOGGER.warn("Public-key payload username \"{}\" does not match the user who sent it!", payload.user);
                    context.player().sendMessage(Text.literal("Internal error occurred trying to bind key.").formatted(Formatting.RED));
                    return;
                }

                if (AuthorisedKeys.bind(payload.user, KeyEncode.encodePublic(payload.publicKey), true))
                    context.player().sendMessage(Text.literal("Rebound your new key to your username!").formatted(Formatting.GREEN));
                else
                    context.player().sendMessage(Text.literal("Your new key has been bound to your username!").formatted(Formatting.GREEN));
            });
        }
    }
}
