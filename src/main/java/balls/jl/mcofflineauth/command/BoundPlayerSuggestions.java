package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.AuthorisedKeys;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class BoundPlayerSuggestions implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        AuthorisedKeys.KEYS.forEach((user, key) -> builder.suggest(user));
        builder.suggest("--");
        return builder.buildFuture();
    }
}
