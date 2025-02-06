package balls.jl.mcofflineauth.mixin;

import balls.jl.mcofflineauth.AuthorisedKeys;
import balls.jl.mcofflineauth.MCOfflineAuth;
import balls.jl.mcofflineauth.ServerConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Unique
    private final PlayerManager players = (PlayerManager) (Object) this;

    @Inject(method = "checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress addr, GameProfile profile, CallbackInfoReturnable<Text> ret) {
        if (!ServerConfig.isEnforcing()) return;

        String user = profile.getName();
        PlayerEntity existingPlayer = players.getPlayer(user);

        if (existingPlayer != null && AuthorisedKeys.KEYS.containsKey(user))
            ret.setReturnValue(Text.literal("A player with that user is already online, and the user is bound."));
    }
}
