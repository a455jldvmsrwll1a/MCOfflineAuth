package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.IgnoredUsers;
import balls.jl.mcofflineauth.UUIDRemap;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class UUIDRemapSuggestions implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        UUIDRemap.REMAPS.forEach((src, dest) -> builder.suggest(src.toString(), new LiteralMessage("%s -> %s".formatted(src, dest))));
        return builder.buildFuture();
    }
}