package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.*;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import balls.jl.mcofflineauth.util.KeyEncode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

import static balls.jl.mcofflineauth.AuthorisedKeys.KEYS;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);
    private static final int OK = 1;
    private static final int FAIL = 0;

    private static int printModInfo(CommandContext<ServerCommandSource> context) {
        var src = context.getSource();
        if (src.isExecutedByPlayer())
            src.sendFeedback(() -> Text.literal("§d§kBALLS§r §lMC §b§lOffline Auth §f(§hFabric§f)§r §d§kBALLS§r"), false);
        else src.sendFeedback(() -> Text.literal("============= MC Offline Auth (Fabric) ============="), false);
        src.sendFeedback(() -> Text.literal("Written by jldmw1a").formatted(Formatting.GREEN, Formatting.ITALIC), false);
        src.sendFeedback(() -> Text.literal("Type '/offauth help' for available commands or '/offauth info' for mod status."), false);
        return OK;
    }

    private static int printHelp(CommandContext<ServerCommandSource> context) {
        var src = context.getSource();
        boolean op = src.hasPermissionLevel(4);
        boolean privileged = op || Permissions.check(src, "mc-offline-auth");
        boolean player = src.isExecutedByPlayer();

        StringBuilder sb = new StringBuilder();

        if (player)
            sb.append("§5===== MC Offline Auth (Fabric) =====§r\n");
        else
            sb.append("===== MC Offline Auth (Fabric) =====\n");

        sb.append("/offauth help\n");

        sb.append("/offauth info.\n");
        if (privileged)
            sb.append("/offauth info <user>\n");

        sb.append("/offauth bind\n");
        if (privileged)
            sb.append("/offauth bind <user> <key>\n");

        sb.append("/offauth unbind\n");
        if (privileged) {
            sb.append("/offauth unbind <user>\n");
            if (player)
                sb.append("/offauth unbind -- §7// all players§r\n");
            else
                sb.append("/offauth unbind -- // all players\n");
        }

        if (privileged) {
            sb.append("""
                    /offauth (enable | disable)
                    /offauth reload
                    /offauth ignore uuid <uuid>
                    /offauth ignore name <username>
                    /offauth unignore uuid <uuid>
                    /offauth unignore name <username>
                    /offauth grace <user>
                    """);

            if (player)
                sb.append("/offauth grace -- §7// all players§r");
            else
                sb.append("/offauth grace -- // all players");
        } else {
            if (player) {
                sb.append("§6OP-only commands are hidden.§r");
            } else {
                sb.append("** OP-only commands are hidden. **");
            }
        }

        src.sendFeedback(() -> Text.literal(sb.toString()), false);

        return OK;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment environment) {
        if (environment.integrated) return;

        dispatcher.register(literal("offauth").executes(Commands::printModInfo).then(literal("info").executes(context -> {
            printModInfo(context);
            var src = context.getSource();

            if (ServerConfig.isEnforcing()) {
                if (src.isExecutedByPlayer())
                    src.sendFeedback(() -> Text.literal("Authentication: §a§lENFORCED§r"), false);
                else src.sendFeedback(() -> Text.literal("Authentication: ENFORCED"), false);
            } else {
                if (src.isExecutedByPlayer())
                    src.sendFeedback(() -> Text.literal("Authentication: §c§lSTANDBY§r"), false);
                else src.sendFeedback(() -> Text.literal("Authentication: STANDBY"), false);
            }

            if (src.isExecutedByPlayer())
                src.sendFeedback(() -> Text.literal("§e%s§r users in the database.".formatted(KEYS.size())), false);
            else src.sendFeedback(() -> Text.literal("%s users in the database.".formatted(KEYS.size())), false);

            return OK;
        }).then(argument("user", StringArgumentType.word()).requires(Permissions.require("mc-offline-auth", 4)).suggests(new PlayerSuggestions()).executes(context -> {
            var src = context.getSource();
            String user = StringArgumentType.getString(context, "user");
            var entry = KEYS.get(user);
            if (entry != null) {
                String key = KeyEncode.encodePublic(entry);
                if (src.isExecutedByPlayer())
                    src.sendFeedback(() -> Text.literal("User \"%s\" has key: §b%s§r".formatted(user, key)).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal("Copy to clipboard."))).withClickEvent(new ClickEvent.CopyToClipboard(key))), false);
                else src.sendFeedback(() -> Text.literal("User \"%s\" has key: %s".formatted(user, key)), false);
            } else {
                src.sendFeedback(() -> Text.literal("User \"%s\" has no bound key.".formatted(user)).formatted(Formatting.RED), false);
                src.sendFeedback(() -> Text.literal("You can bind a key by running /offauth bind %s <pubKey>.".formatted(user)), false);
            }

            return OK;
        }))).then(literal("help").executes(Commands::printHelp)).then(literal("list").executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("%s known users:".formatted(KEYS.size())), false);
            KEYS.forEach((user, key) -> context.getSource().sendFeedback(() -> Text.literal("  + %s".formatted(user)), false));
            return OK;
        })).then(literal("enable").requires(Permissions.require("mc-offline-auth", 4)).executes(context -> {
            if (!ServerConfig.setEnforcing(true)) {
                context.getSource().sendFeedback(() -> Text.literal("Authentication is already active.").formatted(Formatting.RED), false);
                return FAIL;
            }
            ServerConfig.write();
            LOGGER.info("Offline Auth now enforcing.");
            context.getSource().sendFeedback(() -> Text.literal("MC Offline Auth is now ENFORCING.").formatted(Formatting.BLUE), true);
            return OK;
        })).then(literal("reload").requires(Permissions.require("mc-offline-auth", 4)).executes(context -> {
            LOGGER.info("Reloading user-key listing and config from disk...");
            ServerConfig.read();
            IgnoredUsers.read();
            AuthorisedKeys.read();
            context.getSource().sendFeedback(() -> Text.literal("MCOfflineAuth reloaded!.").formatted(Formatting.GRAY), true);
            return OK;
        })).then(literal("disable").requires(Permissions.require("mc-offline-auth", 4)).executes(context -> {
            if (!ServerConfig.setEnforcing(false)) {
                context.getSource().sendFeedback(() -> Text.literal("Authentication is already inactive.").formatted(Formatting.RED), false);
                return FAIL;
            }
            ServerConfig.write();
            LOGGER.warn("Offline auth now on standby.");
            context.getSource().sendFeedback(() -> Text.literal("MC Offline Auth is now ON STANDBY.").formatted(Formatting.DARK_RED), true);
            return OK;
        })).then(literal("bind").executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFeedback(() -> Text.literal("Binding without specifying a user can only be done by players."), false);
                context.getSource().sendFeedback(() -> Text.literal("Use /offauth bind <user> <key> to bind a specific user."), false);
                return FAIL;
            }

            context.getSource().sendFeedback(() -> Text.literal("Trying to bind key; this won't work without the mod installed.").formatted(Formatting.GRAY), false);
            ServerPlayNetworking.send(player, new PubkeyQueryPayload());
            return OK;
        }).then(argument("user", StringArgumentType.word()).requires(Permissions.require("mc-offline-auth", 4)).suggests(new PlayerSuggestions()).then(argument("public-key", StringArgumentType.word()).executes(context -> {
            String user = StringArgumentType.getString(context, "user");
            String key = StringArgumentType.getString(context, "public-key");
            try {
                switch (AuthorisedKeys.bind(user, key, true)) {
                    case INSERTED -> context.getSource().sendFeedback(() -> Text.literal("Bound key to user %s.".formatted(user)).formatted(Formatting.GREEN), true);
                    case IDENTICAL -> context.getSource().sendFeedback(() -> Text.literal("This key is already bound to user %s").formatted(Formatting.RED), false);
                    case REPLACED -> context.getSource().sendFeedback(() -> Text.literal("Replaced the key bound to user %s.".formatted(user)).formatted(Formatting.GREEN), true);
                }
                return OK;
            } catch (IllegalArgumentException e) {
                context.getSource().sendFeedback(() -> Text.literal("!! Provided public key is invalid! Error:").formatted(Formatting.RED), false);
                context.getSource().sendFeedback(() -> Text.literal("!! %s".formatted(e.toString())).formatted(Formatting.DARK_GRAY), false);
                context.getSource().sendFeedback(() -> Text.literal("!! Tip: make sure you copy-pasted the full public key."), false);
                return FAIL;
            }
        })))).then(literal("unbind").executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFeedback(() -> Text.literal("Unbinding without specifying a user can only be done by players."), false);
                context.getSource().sendFeedback(() -> Text.literal("Use /offauth unbind <user> to unbind a specific user."), false);
                return FAIL;
            }

            String user = player.getName().getString();
            if (AuthorisedKeys.unbind(user, true)) {
                MCOfflineAuth.UNBOUND_USER_GRACES.hold(user);
                context.getSource().sendFeedback(() -> Text.literal("Unbound your key.").formatted(Formatting.GREEN), true);
                if (!ServerConfig.allowsUnboundUsers())
                    context.getSource().sendFeedback(() -> Text.literal("The server will reject users without a key, but you have a %ss grace period to bind again.".formatted(ServerConfig.getUnboundUserGracePeriod())).formatted(Formatting.GOLD), false);
                return OK;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("You haven't bound your key yet.").formatted(Formatting.RED), false);
                return FAIL;
            }
        }).then(argument("user", StringArgumentType.word()).requires(Permissions.require("mc-offline-auth", 4)).suggests(new BoundPlayerSuggestions()).executes(context -> {
            String user = StringArgumentType.getString(context, "user");

            if (Objects.equals(user, "--")) {
                AuthorisedKeys.clear(true);
                context.getSource().sendFeedback(() -> Text.literal("Unbound ALL users!").formatted(Formatting.GREEN, Formatting.BOLD), true);
                if (!ServerConfig.allowsUnboundUsers())
                    context.getSource().sendFeedback(() -> Text.literal("Note: unbind grace periods do not apply here.").formatted(Formatting.GOLD), false);
                return OK;
            }

            if (AuthorisedKeys.unbind(user, true)) {
                MCOfflineAuth.UNBOUND_USER_GRACES.hold(user);
                context.getSource().sendFeedback(() -> Text.literal("Unbound user %s.".formatted(user)).formatted(Formatting.GREEN), true);
                if (!ServerConfig.allowsUnboundUsers())
                    context.getSource().sendFeedback(() -> Text.literal("Note: there is a %ss grace period where user %s can still join.".formatted(ServerConfig.getUnboundUserGracePeriod(), user)).formatted(Formatting.GOLD), false);
                return OK;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("No such user %s has a key bound.".formatted(user)).formatted(Formatting.RED), false);
                return FAIL;
            }
        }))).then(literal("grace").requires(Permissions.require("mc-offline-auth", 4)).then(argument("user", StringArgumentType.word()).suggests(new BoundPlayerSuggestions()).executes(context -> {
            String user = StringArgumentType.getString(context, "user");
            MCOfflineAuth.UNBOUND_USER_GRACES.hold(user);
            if (Objects.equals(user, "--"))
                context.getSource().sendFeedback(() -> Text.literal("Set grace period of %ss.".formatted(ServerConfig.getUnboundUserGracePeriod())).formatted(Formatting.GREEN), true);
            else
                context.getSource().sendFeedback(() -> Text.literal("Set grace period of %ss for user %s.".formatted(ServerConfig.getUnboundUserGracePeriod(), user)).formatted(Formatting.GREEN), true);

            return OK;
        }))).then(literal("ignore").requires(Permissions.require("mc-offline-auth", 4)).then(literal("uuid").then(argument("uuid", UuidArgumentType.uuid()).executes(context -> {
            UUID uuid = UuidArgumentType.getUuid(context, "uuid");
            if (!IgnoredUsers.ignoreUUID(uuid)) {
                context.getSource().sendFeedback(() -> Text.literal("This UUID is already in the ignore list.").formatted(Formatting.RED), false);
                return FAIL;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("Player %s will be exempt from authentication.".formatted(uuid)).formatted(Formatting.GREEN), true);
                return OK;
            }
        }))).then(literal("name").then(argument("name", StringArgumentType.word()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            if (!IgnoredUsers.ignoreUsername(name)) {
                context.getSource().sendFeedback(() -> Text.literal("This username is already in the ignore list.").formatted(Formatting.RED), false);
                context.getSource().sendFeedback(() -> Text.literal("Player %s will be exempt from authentication.".formatted(name)).formatted(Formatting.GREEN), true);
                return FAIL;
            }

            return OK;
        })))).then(literal("unignore").requires(Permissions.require("mc-offline-auth", 4)).then(literal("uuid").then(argument("uuid", UuidArgumentType.uuid()).suggests(new IgnoredUuidSuggestions()).executes(context -> {
            UUID uuid = UuidArgumentType.getUuid(context, "uuid");
            if (!IgnoredUsers.unignoreUUID(uuid)) {
                context.getSource().sendFeedback(() -> Text.literal("This UUID is not in the ignore list.").formatted(Formatting.RED), false);
                return FAIL;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("Player %s will no longer be exempt from authentication.".formatted(uuid)).formatted(Formatting.GREEN), true);
                return OK;
            }
        }))).then(literal("name").then(argument("name", StringArgumentType.word()).suggests(new IgnoredUsernameSuggestions()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            if (!IgnoredUsers.unignoreUsername(name)) {
                context.getSource().sendFeedback(() -> Text.literal("This username is not in the ignore list.").formatted(Formatting.RED), false);
                return FAIL;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("Player %s will no longer be exempt from authentication.".formatted(name)).formatted(Formatting.GREEN), true);
                return OK;
            }
        })))));
    }
}
