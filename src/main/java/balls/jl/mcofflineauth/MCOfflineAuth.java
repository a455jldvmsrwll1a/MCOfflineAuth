package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.command.Commands;
import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import balls.jl.mcofflineauth.util.KeyEncode;
import com.mojang.authlib.GameProfile;
import lol.bai.badpackets.api.PacketReceiver;
import lol.bai.badpackets.api.config.ConfigPackets;
import lol.bai.badpackets.api.config.ConfigTaskExecutor;
import lol.bai.badpackets.api.config.ServerConfigContext;
import lol.bai.badpackets.api.play.PlayPackets;
import lol.bai.badpackets.api.play.ServerPlayContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MCOfflineAuth implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private static final ChallengeManager CHALLENGES = new ChallengeManager();

    public static final UnboundUserGraces UNBOUND_USER_GRACES = new UnboundUserGraces();

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

        ServerTickEvents.END_SERVER_TICK.register(MCOfflineAuth::onServerTickFinished);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();

        if (!server.isSingleplayer() && !AuthorisedKeys.KEYS.containsKey(player.getName().getString())) {
            ServerConfig.print("noKeyBannerHeader", (msg) -> player.sendMessage(Text.of(msg)));
            ServerConfig.print("noKeyBannerInfo", (msg) -> player.sendMessage(Text.of(msg)));
            ServerConfig.print("noKeyBannerHint", (msg) -> player.sendMessage(Text.literal(msg).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click --> /offauth bind"))).withClickEvent(new ClickEvent.RunCommand("/offauth bind")))));

            if (!ServerConfig.allowsUnboundUsers() && UNBOUND_USER_GRACES.isHeld(player.getName().getString()))
                ServerConfig.print("noKeyGrace", (msg) -> player.sendMessage(Text.of(msg)));
        }
    }

    private static void onServerTickFinished(MinecraftServer server) {
        CHALLENGES.removeExpired();
        UNBOUND_USER_GRACES.removeExpired();
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
        IgnoredUsers.read();
        AuthorisedKeys.read();
    }

    static class LoginTask implements ConfigTaskExecutor {

        @Override
        public boolean runTask(ServerConfigContext context) {
            if (!ServerConfig.isEnforcing()) {
                LOGGER.warn("MCOfflineAuth is on standby; won't do anything here.");
                return false;
            }

            GameProfile profile = context.handler().getDebugProfile();
            if (IgnoredUsers.playerIsIgnored(profile)) {
                LOGGER.warn("Player is exempt from authentication.");
                return false;
            }

            String username = profile.getName();

            if (!context.canSend(LoginChallengePayload.ID)) {
                context.handler().onDisconnected(new DisconnectionInfo(Text.of("Client does not have MCOfflineAuth installed.")));
                warn_unauthorised_login(context.server(), username, "doesn't have MCOA mod");
                context.handler().disconnect(Text.of(ServerConfig.message("accessDenied")));
                return false;
            }

            LoginChallengePayload payload = CHALLENGES.createChallenge(username);
            context.send(payload);
            return true;
        }
    }

    static class LoginResponseReceiver implements PacketReceiver<lol.bai.badpackets.api.config.ServerConfigContext, balls.jl.mcofflineauth.net.LoginResponsePayload> {

        @Override
        public void receive(ServerConfigContext context, LoginResponsePayload payload) {
            context.finishTask(Constants.LOGIN_TASK);

            Challenge state = CHALLENGES.remove(payload.id);
            if (state == null) {
                context.handler().disconnect(Text.of(ServerConfig.message("timeout")));
                return;
            }

            if (!AuthorisedKeys.KEYS.containsKey(state.user)) {
                LOGGER.warn("Connecting user {} is not in the database.", state.user);

                // Skip verification for unbound usernames if it's allowed.
                if (ServerConfig.allowsUnboundUsers()) return;

                if (UNBOUND_USER_GRACES.isHeld(state.user)) {
                    LOGGER.warn("Unbound users cannot join but user {} will be exempted via unbind grace period.", state.user);
                    return;
                }

                // Else, kick them.
                warn_unauthorised_login(context.server(), state.user, "not bound");
                context.handler().disconnect(Text.of(ServerConfig.message("kickNoKey")));
                return;
            }

            if (!AuthorisedKeys.verifySignature(state.user, state.data, Uuids.toByteArray(payload.id), payload.signature)) {
                warn_unauthorised_login(context.server(), state.user, "wrong signature/key; can't verify identity");
                context.handler().disconnect(Text.of(ServerConfig.message("wrongIdentity")));
                return;
            }

            LOGGER.info("Verified {}'s identity successfully.", state.user);
        }
    }

    static class PubkeyBindReceiver implements PacketReceiver<lol.bai.badpackets.api.play.ServerPlayContext, balls.jl.mcofflineauth.net.PubkeyBindPayload> {

        @Override
        public void receive(ServerPlayContext context, PubkeyBindPayload payload) {
            context.server().execute(() -> {
                if (!Objects.equals(payload.user, context.player().getName().getString())) {
                    LOGGER.warn("Public-key payload username \"{}\" does not match the user who sent it!", payload.user);
                    context.player().sendMessage(Text.literal("Internal error occurred trying to bind key.").formatted(Formatting.RED));
                    return;
                }

                switch (AuthorisedKeys.bind(payload.user, KeyEncode.encodePublic(payload.publicKey), true)) {
                    case INSERTED -> context.player().sendMessage(Text.literal("Your new key has been bound to your username!").formatted(Formatting.GREEN));
                    case IDENTICAL -> context.player().sendMessage(Text.literal("You have already bound this key.").formatted(Formatting.RED));
                    case REPLACED -> context.player().sendMessage(Text.literal("Rebound your new key to your username!").formatted(Formatting.GREEN));
                }
            });
        }
    }

    static void warn_unauthorised_login(MinecraftServer server, String user, String reason) {
        if (!ServerConfig.warnsUnauthorisedLogins()) return;

        server.execute(() -> server.getPlayerManager().getPlayerList().forEach(player -> {
            try {
                if (player.hasPermissionLevel(1) || Permissions.check(player.getUuid(), "mc-offline-auth").get()) {
                    var style = Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal("MCOfflineAuth rejected this player.")));
                    player.sendMessage(Text.literal(ServerConfig.message("rejectWarn").formatted(user, reason)).setStyle(style));
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
