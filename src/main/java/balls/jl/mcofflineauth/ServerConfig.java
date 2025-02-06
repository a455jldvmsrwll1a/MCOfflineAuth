package balls.jl.mcofflineauth;

public class ServerConfig {
    private static boolean AUTH_ENFORCING = true;
    private static boolean ALLOW_UNBOUND_USERS = true;

    public static boolean isEnforcing() {
        return AUTH_ENFORCING;
    }

    public static boolean allowsUnboundUsers() {
        return ALLOW_UNBOUND_USERS;
    }

    public static boolean setEnforcing(boolean enforce) {
        if (AUTH_ENFORCING == enforce)
            return false;

        AUTH_ENFORCING = enforce;
        return true;
    }

    public static boolean setAllowUnboundUsers(boolean allow) {
        if (ALLOW_UNBOUND_USERS == allow)
            return false;

        ALLOW_UNBOUND_USERS = allow;
        return true;
    }
}
