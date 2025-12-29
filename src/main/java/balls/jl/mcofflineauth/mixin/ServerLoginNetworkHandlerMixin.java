package balls.jl.mcofflineauth.mixin;

import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.IgnoredUsers;
import balls.jl.mcofflineauth.ServerConfig;
import balls.jl.mcofflineauth.UUIDRemap;
import com.mojang.authlib.GameProfile;
import java.security.PrivateKey;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Final
    ClientConnection connection;

    @Shadow
    @Nullable
    String profileName;

    @Shadow
    @Final
    private byte[] nonce;

    @Shadow
    private ServerLoginNetworkHandler.State state;

    @Shadow
    private @org.jspecify.annotations.Nullable GameProfile profile;

    @Unique
    private boolean useNormalAuthentication = false;

    @Shadow
    abstract void startVerify(GameProfile profile);

    @Inject(method = "onHello", at = @At("HEAD"), cancellable = true)
    private void handleIncoming(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if (!ServerConfig.keepingEncryption() || !ServerConfig.isEnforcing()) {
            // use normal authentication
            return;
        }

        if (state != ServerLoginNetworkHandler.State.HELLO) {
            throw new IllegalStateException("Got a hello packet in the middle of the login!");
        }

        profileName = packet.name();

        if (!StringHelper.isValidPlayerName(profileName)) {
            throw new IllegalStateException("Username has invalid characters!");
        }

        if (IgnoredUsers.playerIsIgnored(new GameProfile(packet.profileId(), profileName))) {
            useNormalAuthentication = true;
            return;
        }

        GameProfile hostProfile = server.getHostProfile();
        // skip if player *is* the host.
        if (hostProfile != null && hostProfile.name().equalsIgnoreCase(profileName)) {
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
        if (useNormalAuthentication || !ServerConfig.keepingEncryption() || !ServerConfig.isEnforcing()) {
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

    @Inject(method = "startVerify", at = @At("TAIL"))
    private void startVerify(GameProfile profile, CallbackInfo ci) {
        UUID replacementUUID = UUIDRemap.REMAPS.get(profile.id());
        if (replacementUUID != null) {
            this.profile = new GameProfile(replacementUUID, profile.name());

            LOGGER.info("Remapped incoming UUID {} to instead be {}.", profile.id(), replacementUUID);
        }
    }
}
