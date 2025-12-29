package balls.jl.mcofflineauth;

import static balls.jl.mcofflineauth.MCOfflineAuth.bindUserKey;

import balls.jl.mcofflineauth.util.KeyEncode;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyChangeRequests {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private final ConcurrentHashMap<String, Request> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PublicKey> acceptedRequests = new ConcurrentHashMap<>();

    public Iterator<String> usersAwaitingApproval() {
        return pendingRequests.keySet().stream().iterator();
    }

    /// Create a new request.
    /// @param user Username associated with the request.
    /// @param key Public key associated with the request.
    /// @return `false` if a request with the same user and key already exists. `true` otherwise.
    public boolean requestStore(@NonNull String user, @NonNull PublicKey key) {
        LOGGER.info("Incoming key store request from user {} with key {}", user, KeyEncode.encodePublic(key));

        if (pendingRequests.get(user) != null && pendingRequests.get(user).key().equals(key)) {
            LOGGER.info("Incoming request is a duplicate.");
            return false;
        }

        Instant deadline = Instant.now().plusSeconds(300);
        pendingRequests.put(user, new Request(key, deadline));

        return true;
    }

    public void requestDrop(String user) {
        LOGGER.info("Incoming key drop request from user {}", user);

        Instant deadline = Instant.now().plusSeconds(300);
        pendingRequests.put(user, new Request(null, deadline));
    }

    public boolean approveUser(String user) {
        synchronized (this) {
            Request approved = pendingRequests.remove(user);
            if (approved != null) {
                acceptedRequests.put(user, approved.key);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean rejectUser(String user) {
        return pendingRequests.remove(user) != null;
    }

    public void removeExpired() {
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public void applyAcceptedRequests(MinecraftServer server) {
        HashMap<String, PublicKey> requests = new HashMap<>(acceptedRequests);

        requests.forEach((user, key) -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(user);
            if (player != null) {
                bindUserKey(server, player, key);
            }
        });

        acceptedRequests.clear();
    }

    public Enumeration<String> getRequests() {
        return pendingRequests.keys();
    }

    private record Request(PublicKey key, Instant deadline) {
        boolean isExpired() {
            return Instant.now().isAfter(deadline);
        }
    }
}
