package balls.jl.mcofflineauth;

import java.net.SocketAddress;
import java.time.Instant;

public class Challenge {
    static final int CHALLENGE_TIMEOUT = 5000;
    public final String user;
    public final SocketAddress address;
    public final byte[] data;
    private final Instant expiration;

    public Challenge(String user, SocketAddress address, byte[] data) {
        expiration = Instant.now().plusMillis(CHALLENGE_TIMEOUT);
        this.user = user;
        this.address = address;
        this.data = data;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiration);
    }
}
