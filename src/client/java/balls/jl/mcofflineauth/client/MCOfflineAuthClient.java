package balls.jl.mcofflineauth.client;

import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import lol.bai.badpackets.api.PacketReceiver;
import lol.bai.badpackets.api.config.ClientConfigContext;
import lol.bai.badpackets.api.config.ConfigPackets;
import lol.bai.badpackets.api.play.ClientPlayContext;
import lol.bai.badpackets.api.play.PlayPackets;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.network.ClientPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class MCOfflineAuthClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static boolean SHOW_HELP_TOAST;

    private static void registerEventCallbacks() {
        ConfigPackets.registerClientReceiver(LoginChallengePayload.ID, new LoginChallengeReceiver());
        PlayPackets.registerClientReceiver(PubkeyQueryPayload.ID, new PubkeyQueryReceiver());
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initialising MCOfflineAuth::Client. (on Fabric)");

        registerEventCallbacks();
        ClientKeyPair.loadOrCreate();
    }

    static class LoginChallengeReceiver implements PacketReceiver<ClientConfigContext, LoginChallengePayload> {

        @Override
        public void receive(ClientConfigContext context, LoginChallengePayload payload) {
            context.client().execute(() -> {
                try {
                    Signature sig = Signature.getInstance(Constants.ALGORITHM);
                    sig.initSign(ClientKeyPair.KEY_PAIR.getPrivate());
                    sig.update(payload.data);

                    context.send(new LoginResponsePayload(payload.id, sig.sign()));
                } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                    throw new RuntimeException(e);
                }

            });
        }
    }

    static class PubkeyQueryReceiver implements PacketReceiver<ClientPlayContext, PubkeyQueryPayload> {

        @Override
        public void receive(ClientPlayContext context, PubkeyQueryPayload payload) {
            context.client().execute(() -> {
                ClientPlayerEntity player = context.client().player;
                if (player != null && player.getName() != null) {
                    String user = player.getName().getString();
                    context.send(new PubkeyBindPayload(user, ClientKeyPair.KEY_PAIR.getPublic()));
                } else {
                    LOGGER.error("Failed to send public key to the server.");
                }
            });
        }
    }
}

