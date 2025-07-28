package balls.jl.mcofflineauth;

import balls.jl.mcofflineauth.net.LoginChallengePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public class ChallengeManager {
    private static final int MAX_CHALLENGES = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private final LinkedHashMap<UUID, Challenge> challenges = new LinkedHashMap<>();
    private final HashMap<String, UUID> trackedUsers = new HashMap<>();

    public synchronized LoginChallengePayload createChallenge(String user) {
        SecureRandom rng = new SecureRandom();
        UUID uuid = new UUID(rng.nextLong(), rng.nextLong());

        byte[] plainText = new byte[512];
        rng.nextBytes(plainText);

        LoginChallengePayload payload = new LoginChallengePayload(uuid, plainText, user);
        challenges.put(uuid, new Challenge(user, plainText));

        UUID oldChallenge = trackedUsers.put(user, uuid);
        if (oldChallenge != null)
            challenges.remove(oldChallenge);

        if (challenges.size() > MAX_CHALLENGES) {
            Challenge old = challenges.remove(challenges.firstEntry().getKey());
            trackedUsers.remove(old.user);
        }

        return payload;
    }

    public synchronized Challenge remove(UUID id) {
        Challenge removed = challenges.remove(id);
        trackedUsers.remove(removed.user);
        return removed;
    }

    public synchronized void removeExpired() {
        challenges.entrySet().removeIf(entry -> {
            Challenge state = entry.getValue();
            boolean expired = state.isExpired();

            if (expired) {
                trackedUsers.remove(state.user);
                LOGGER.warn("Challenge expired for connecting user {}: {}", state.user, entry.getKey());
            }
            return expired;
        });
    }
}
