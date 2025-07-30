package balls.jl.mcofflineauth;

import net.minecraft.util.Pair;

import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeyChangeRequests {
    private final ConcurrentHashMap<String, Request> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PublicKey> acceptedRequests = new ConcurrentHashMap<>();

    public Iterator<String> usersAwaitingApproval() {
        return pendingRequests.keySet().stream().iterator();
    }

    public void requestStore(String user, PublicKey key) {
        Instant deadline = Instant.now().plusSeconds(300);
        pendingRequests.put(user, new Request(key, deadline));
    }

    public void requestDrop(String user) {
        requestStore(user, null);
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

    public Optional<List<Map.Entry<String, PublicKey>>> takeAcceptedRequests() {
        if (acceptedRequests.isEmpty())
            return Optional.empty();

        List<Map.Entry<String, PublicKey>> requests = new ArrayList<>(acceptedRequests.entrySet());
        acceptedRequests.clear();

        return Optional.of(requests);
    }

    private record Request(PublicKey key, Instant deadline) {
        boolean isExpired() {
            return Instant.now().isAfter(deadline);
        }
    }
}
