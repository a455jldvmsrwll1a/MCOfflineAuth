package balls.jl.mcofflineauth.client;

import balls.jl.mcofflineauth.Constants;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCOfflineAuthClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static boolean SHOW_HELP_TOAST;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initialising MCOfflineAuth::Client. (on Fabric)");

        registerEventCallbacks();
        ClientKeyPair.loadOrCreate();
    }

    private void registerEventCallbacks() {
        LOGGER.info("debug: registerEventCallbacks()");
    }
}
