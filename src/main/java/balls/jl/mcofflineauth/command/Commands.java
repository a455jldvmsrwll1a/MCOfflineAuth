package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.AuthorisedKeys;
import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.ServerConfig;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import balls.jl.mcofflineauth.util.KeyEncode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
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
        src.sendFeedback(() -> Text.literal("Written by JL :>").formatted(Formatting.GREEN, Formatting.ITALIC), false);
        src.sendFeedback(() -> Text.literal("Warning: this mod is alpha software. Please report any issues found.").formatted(Formatting.GOLD), false);
        src.sendFeedback(() -> Text.literal("Type '/offauth help' for available commands or '/offauth info' for mod status."), false);
        return OK;
    }

    private static int printPriveledgedHelp(CommandContext<ServerCommandSource> context) {
        var src = context.getSource();
        src.sendFeedback(() -> Text.literal("============= MC Offline Auth (Fabric) =============").formatted(Formatting.DARK_PURPLE), false);
        src.sendFeedback(() -> Text.literal("/offauth info                      - Show information about MCOA."), false);
        src.sendFeedback(() -> Text.literal("/offauth info <user>               - Show info about this <user>."), false);
        src.sendFeedback(() -> Text.literal("/offauth help                      - Show this help text."), false);
        src.sendFeedback(() -> Text.literal("/offauth list                      - List known users."), false);
        src.sendFeedback(() -> Text.literal("/offauth enable                    - Enable authentication."), false);
        src.sendFeedback(() -> Text.literal("/offauth disable                   - Disable authentication."), false);
        src.sendFeedback(() -> Text.literal("/offauth reload                    - Reload the authorised user list."), false);
        src.sendFeedback(() -> Text.literal("/offauth bind                      - Bind your key to your user."), false);
        src.sendFeedback(() -> Text.literal("/offauth bind <user> <key>         - Bind given <key> to <user>."), false);
        src.sendFeedback(() -> Text.literal("/offauth unbind <user>             - Unbind yourself, or the user <user>."), false);
        src.sendFeedback(() -> Text.literal("/offauth allowUnboundUsers <allow> - Set whether to allow users without a key bound."), false);
        return OK;
    }

    private static int printHelp(CommandContext<ServerCommandSource> context) {
        var src = context.getSource();
        src.sendFeedback(() -> Text.literal("============= MC Offline Auth (Fabric) =============").formatted(Formatting.DARK_PURPLE), false);
        src.sendFeedback(() -> Text.literal("/offauth info   - Show information about MCOA."), false);
        src.sendFeedback(() -> Text.literal("/offauth help   - Show this help text."), false);
        src.sendFeedback(() -> Text.literal("/offauth bind   - Bind your key to your user."), false);
        src.sendFeedback(() -> Text.literal("/offauth unbind - Unbind yourself."), false);
        src.sendFeedback(() -> Text.literal("Missing commands? Some of them are OP-only.").formatted(Formatting.GOLD), false);
        return OK;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment environment) {
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
        }).then(argument("user", StringArgumentType.word()).requires(source -> source.hasPermissionLevel(4)).suggests(new PlayerSuggestions()).executes(context -> {
            var src = context.getSource();
            String user = StringArgumentType.getString(context, "user");
            var entry = KEYS.get(user);
            if (entry != null) {
                String key = KeyEncode.encodePublic(entry);
                if (src.isExecutedByPlayer())
                    src.sendFeedback(() -> Text.literal("User \"%s\" has key: §b%s§r".formatted(user, key)).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Copy to clipboard."))).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, key))), false);
                else src.sendFeedback(() -> Text.literal("User \"%s\" has key: %s".formatted(user, key)), false);
            } else {
                src.sendFeedback(() -> Text.literal("User \"%s\" has no bound key.".formatted(user)).formatted(Formatting.RED), false);
                src.sendFeedback(() -> Text.literal("You can bind a key by running /offauth bind %s <pubKey>.".formatted(user)), false);
            }

            return OK;
        }))).then(literal("help").executes(context -> {
            if (context.getSource().hasPermissionLevel(4)) return printPriveledgedHelp(context);
            else return printHelp(context);
        })).then(literal("list").executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("%s known users:".formatted(KEYS.size())), false);
            KEYS.forEach((user, key) -> context.getSource().sendFeedback(() -> Text.literal("  + %s".formatted(user)), false));
            return OK;
        })).then(literal("enable").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            if (ServerConfig.isEnforcing()) {
                context.getSource().sendFeedback(() -> Text.literal("Authentication is already active.").formatted(Formatting.RED), false);
                return FAIL;
            }
            ServerConfig.setEnforcing(true);
            LOGGER.info("Offline Auth now enforcing.");
            context.getSource().sendFeedback(() -> Text.literal("MC Offline Auth is now ENFORCING.").formatted(Formatting.BLUE), true);
            return OK;
        })).then(literal("reload").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            LOGGER.info("Reloading user-key listing from disk...");
            AuthorisedKeys.read();
            context.getSource().sendFeedback(() -> Text.literal("MCOA Database reloaded!.").formatted(Formatting.GRAY), true);
            return OK;
        })).then(literal("disable").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            if (!ServerConfig.isEnforcing()) {
                context.getSource().sendFeedback(() -> Text.literal("Authentication is already inactive.").formatted(Formatting.RED), false);
                return FAIL;
            }
            ServerConfig.setEnforcing(false);
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

            ServerPlayNetworking.send(player, new PubkeyQueryPayload());
            return OK;
        }).then(argument("user", StringArgumentType.word()).requires(source -> source.hasPermissionLevel(4)).suggests(new PlayerSuggestions()).then(argument("public-key", StringArgumentType.word()).executes(context -> {
            String user = StringArgumentType.getString(context, "user");
            String key = StringArgumentType.getString(context, "public-key");
            try {
                if (AuthorisedKeys.bind(user, key, true))
                    context.getSource().sendFeedback(() -> Text.literal("Replaced the key bound to %s.".formatted(user)).formatted(Formatting.GREEN), true);
                else
                    context.getSource().sendFeedback(() -> Text.literal("Bound key to user %s.".formatted(user)).formatted(Formatting.GREEN), true);
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

            if (AuthorisedKeys.unbind(player.getName().getString(), true)) {
                context.getSource().sendFeedback(() -> Text.literal("Unbound your key.").formatted(Formatting.GREEN), true);
                return OK;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("You haven't bound your key yet.").formatted(Formatting.RED), false);
                return FAIL;
            }
        }).then(argument("user", StringArgumentType.word()).requires(source -> source.hasPermissionLevel(4)).suggests(new BoundPlayerSuggestions()).executes(context -> {
            String user = StringArgumentType.getString(context, "user");

            if (Objects.equals(user, "--")) {
                AuthorisedKeys.clear(true);
                context.getSource().sendFeedback(() -> Text.literal("Unbound ALL users!").formatted(Formatting.GREEN, Formatting.BOLD), true);
                return OK;
            }

            if (AuthorisedKeys.unbind(user, true)) {
                context.getSource().sendFeedback(() -> Text.literal("Unbound user %s.".formatted(user)).formatted(Formatting.GREEN), true);
                return OK;
            } else {
                context.getSource().sendFeedback(() -> Text.literal("No such user %s has a key bound.".formatted(user)).formatted(Formatting.RED), false);
                return FAIL;
            }
        }))).then(literal("allowUnboundUsers").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            if (ServerConfig.allowsUnboundUsers())
                context.getSource().sendFeedback(() -> Text.literal("Unbound users are allowed to join.").formatted(Formatting.GOLD), false);
            else
                context.getSource().sendFeedback(() -> Text.literal("Unbound users are prohibited from joining.").formatted(Formatting.DARK_BLUE), false);
            return OK;
        }).then(argument("allow", BoolArgumentType.bool()).executes(context -> {
            boolean allow = BoolArgumentType.getBool(context, "allow");
            if (!ServerConfig.setAllowUnboundUsers(allow)) {
                context.getSource().sendFeedback(() -> Text.literal("Nothing changed.").formatted(Formatting.RED), false);
                return FAIL;
            }

            if (allow)
                context.getSource().sendFeedback(() -> Text.literal("Allowing unbound users.").formatted(Formatting.BLUE), true);
            else
                context.getSource().sendFeedback(() -> Text.literal("Prohibiting unbound users.").formatted(Formatting.BLUE), true);
            return OK;
        }))));
    }
}
