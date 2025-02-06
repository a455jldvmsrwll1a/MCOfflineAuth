package balls.jl.mcofflineauth.command;

import balls.jl.mcofflineauth.AuthorisedKeys;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public class PlayerSuggestions implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        Collection<ServerPlayerEntity> players = PlayerLookup.all(context.getSource().getServer());
        HashSet<String> users = new HashSet<>(players.size() + AuthorisedKeys.count());

        players.forEach(player -> {
            String str = player.getName().getLiteralString();
            if (str != null) users.add(str);
        });

        AuthorisedKeys.KEYS.forEach((user, key) -> users.add(user));
        users.forEach(builder::suggest);

        return builder.buildFuture();
    }
}
