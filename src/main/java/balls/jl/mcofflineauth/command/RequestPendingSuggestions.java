package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.MCOfflineAuth;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class RequestPendingSuggestions implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        MCOfflineAuth.KEY_CHANGE_REQUESTS
                .usersAwaitingApproval()
                .forEachRemaining(builder::suggest);

        return builder.buildFuture();
    }
}