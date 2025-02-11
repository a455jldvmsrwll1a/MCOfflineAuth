package balls.jl.mcofflineauth.util;

import balls.jl.mcofflineauth.ServerConfig;

public class DefaultMessages {
    public static void setDefaultMessages() {
        ServerConfig.setMessage("accessDenied", "Access denied. D:");
        ServerConfig.setMessage("timeout", "Signature verification failed due to timeout. Please try again.");
        ServerConfig.setMessage("wrongIdentity", "Unable to verify your identity (incorrect signature); do you have the correct key?");
        ServerConfig.setMessage("kickNoKey", "You cannot join without being bound in advance. Contact a server admin to let you in.");

        ServerConfig.setMessage("noKeyBannerHeader", "§c§lAttention: this username is unclaimed!§r");
        ServerConfig.setMessage("noKeyBannerInfo", "No key is bound to this username; anyone can join with this name.");
        ServerConfig.setMessage("noKeyBannerHint", "§aClick this text or type \"§f/offauth bind§a\" to bind your key!§r");
    }
}
