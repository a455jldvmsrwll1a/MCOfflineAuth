package balls.jl.mcofflineauth;

import java.net.SocketAddress;
import java.time.Instant;

public record Challenge(String user, SocketAddress address, byte[] data, Instant expiration) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiration);
    }
}
