package balls.jl.mcofflineauth.mixin;

import balls.jl.mcofflineauth.ServerConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow @Final private byte[] nonce;
    @Shadow @Final private MinecraftServer server;
    @Shadow private ServerLoginNetworkHandler.State state;
    @Shadow @Final ClientConnection connection;

    @Shadow abstract void startVerify(GameProfile profile);

    @Shadow @Nullable private String profileName;

    @Inject(method = "onHello", at = @At("HEAD"), cancellable = true)
    private void handleIncoming(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if (!ServerConfig.keepingEncryption() || !ServerConfig.isEnforcing()) {
            // execute original code
            return;
        }

        if (state != ServerLoginNetworkHandler.State.HELLO) {
            throw new IllegalStateException("Got a hello packet in the middle of the login!");
        }

        profileName = packet.name();

        if (!StringHelper.isValidPlayerName(profileName)) {
            throw new IllegalStateException("Username has invalid characters!");
        }

        GameProfile hostProfile = server.getHostProfile();
        // skip if player *is* the host.
        if (hostProfile != null && hostProfile.getName().equalsIgnoreCase(profileName)) {
            startVerify(hostProfile);
        } else {
            if (connection.isLocal() || !server.isOnlineMode()) {
                startVerify(Uuids.getOfflinePlayerProfile(profileName));
            } else {
                state = ServerLoginNetworkHandler.State.KEY;

                var keyBytes = server.getKeyPair().getPublic().getEncoded();
                connection.send(new LoginHelloS2CPacket("", keyBytes, nonce, false));
            }
        }

        ci.cancel();
    }

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void handleEncryption(LoginKeyC2SPacket packet, CallbackInfo ci) {
        if (!ServerConfig.keepingEncryption() || !ServerConfig.isEnforcing()) {
            // execute original code
            return;
        }

        if (state != ServerLoginNetworkHandler.State.KEY) {
            throw new IllegalStateException("Not supposed to receive a key packet!");
        }

        if (profileName == null) {
            throw new IllegalStateException("profileName shouldn't be null!");
        }

        try {
            PrivateKey pvKey = server.getKeyPair().getPrivate();
            if (!packet.verifySignedNonce(nonce, pvKey)) {
                throw new IllegalStateException("Failed to verify nonce.");
            }

            SecretKey secKey = packet.decryptSecretKey(pvKey);
            Cipher cipherA = NetworkEncryptionUtils.cipherFromKey(2, secKey);
            Cipher cipherB = NetworkEncryptionUtils.cipherFromKey(1, secKey);

            connection.setupEncryption(cipherA, cipherB);
            startVerify(Uuids.getOfflinePlayerProfile(profileName));

        } catch (Exception e) {
            throw new IllegalStateException("onKey() setup error: ", e);
        }

        ci.cancel();
    }
}
