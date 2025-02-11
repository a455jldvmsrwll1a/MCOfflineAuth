package balls.jl.mcofflineauth;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public  class UnboundUserGraces {
    final ConcurrentHashMap<String, Instant> graces = new ConcurrentHashMap<>();

    public void hold(String user) {
        graces.put(user, Instant.now().plusSeconds(ServerConfig.getUnboundUserGracePeriod()));
    }

    public boolean isHeld(String user) {
        return graces.containsKey(user);
    }

    public void removeExpired() {
        graces.entrySet().removeIf(entry -> Instant.now().isAfter(entry.getValue()));
    }
}