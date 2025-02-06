package balls.jl.mcofflineauth.client;

import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.net.LoginChallengePayload;
import balls.jl.mcofflineauth.net.LoginResponsePayload;
import balls.jl.mcofflineauth.net.PubkeyBindPayload;
import balls.jl.mcofflineauth.net.PubkeyQueryPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class MCOfflineAuthClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static boolean SHOW_HELP_TOAST;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initialising MCOfflineAuth::Client. (on Fabric)");

        registerEventCallbacks();
        ClientKeyPair.loadOrCreate();
    }

    private static void registerEventCallbacks() {
        ClientPlayNetworking.registerGlobalReceiver(PubkeyQueryPayload.ID, MCOfflineAuthClient::onReceivedKeyQuery);
        ClientConfigurationNetworking.registerGlobalReceiver(LoginChallengePayload.ID, MCOfflineAuthClient::onReceivedLoginChallenge);
    }

    private static void onReceivedKeyQuery(PubkeyQueryPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (context.client().player != null) {
                String user = context.client().player.getName().getString();
                ClientPlayNetworking.send(new PubkeyBindPayload(user, ClientKeyPair.KEY_PAIR.getPublic()));
            } else {
                LOGGER.error("Failed to send public key to the server.");
            }
        });
    }

    private static void onReceivedLoginChallenge(LoginChallengePayload payload, ClientConfigurationNetworking.Context context) {
        context.client().execute(() -> {
            String name = context.client().getGameProfile().getName();
            if (name == null) {
                LOGGER.error("Could not retrieve the username.");
                return;
            }

            try {
                Signature sig = Signature.getInstance(Constants.ALGORITHM);
                sig.initSign(ClientKeyPair.KEY_PAIR.getPrivate());
                sig.update(payload.data);

                ClientConfigurationNetworking.send(new LoginResponsePayload(payload.id, name, sig.sign()));
            } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }

        });
    }
}

