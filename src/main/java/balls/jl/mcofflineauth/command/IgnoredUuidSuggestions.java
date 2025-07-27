package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.IgnoredUsers;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class IgnoredUuidSuggestions implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        IgnoredUsers.getIgnoredUuids().forEach(uuid -> builder.suggest(uuid.toString()));
        return builder.buildFuture();
    }
}