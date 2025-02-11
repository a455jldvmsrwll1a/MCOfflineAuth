package balls.jl.mcofflineauth.util;

import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ConfigSerialise {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private static final String MESSAGE_ID_PREFIX = "msg.";

    public static String serialise() {
        StringBuilder sb = new StringBuilder();
        if (ServerConfig.isEnforcing()) sb.append("enforcing = true\n");
        else sb.append("enforcing = false\n");

        if (ServerConfig.allowsUnboundUsers()) sb.append("allow_unbound_users = true\n");
        else sb.append("allow_unbound_users = false\n");

        sb.append("unbound_user_grace_period = ");
        sb.append(ServerConfig.getUnboundUserGracePeriod());
        sb.append('\n');

        ServerConfig.messages().forEach((id, msg) -> sb.append("%s%s = %s\n".formatted(MESSAGE_ID_PREFIX, id, msg)));

        return sb.toString();
    }

    public static void deserialise(String contents) {
        contents.lines().forEach(line -> {
            if (line.startsWith("#")) return;

            // not quite ideal code
            if (line.startsWith(MESSAGE_ID_PREFIX)) deserialiseStringConfig(line);
            else if (line.startsWith("unbound_user_grace_period")) deserialiseIntConfig(line);
            else deserialiseBoolConfig(line);
        });
    }

    private static void deserialiseBoolConfig(String line) {
        String[] tokens = line.strip().split("\\s+");
        if (tokens.length != 3) {
            LOGGER.error("Config line has invalid number of tokens: \"{}\".", line);
            return;
        }

        if (!Objects.equals(tokens[1], "=")) {
            LOGGER.error("Config line is invalid: \"{}\".", line);
            return;
        }

        boolean value;
        if (Objects.equals(tokens[2], "true")) {
            value = true;
        } else if (Objects.equals(tokens[2], "false")) {
            value = false;
        } else {
            LOGGER.error("Config line has invalid value \"{}\": \"{}\".", tokens[2], line);
            return;
        }

        if (Objects.equals(tokens[0], "enforcing")) {
            ServerConfig.setEnforcing(value);
        } else if (Objects.equals(tokens[0], "allow_unbound_users")) {
            ServerConfig.setAllowUnboundUsers(value);
        } else {
            LOGGER.error("Config line has invalid key \"{}\": \"{}\".", tokens[0], line);
        }
    }

    private static void deserialiseIntConfig(String line) {
        String[] tokens = line.strip().split("\\s+");
        if (tokens.length != 3) {
            LOGGER.error("Config line has invalid number of tokens: \"{}\".", line);
            return;
        }

        if (!Objects.equals(tokens[1], "=")) {
            LOGGER.error("Config line is invalid: \"{}\".", line);
            return;
        }

        try {
            int value = Integer.parseInt(tokens[2]);
            if (value < 0) throw new IllegalArgumentException("Negative values are not valid in this context.");

            if (Objects.equals(tokens[0], "unbound_user_grace_period")) {
                ServerConfig.setUnboundUserGracePeriod(value);
            } else {
                LOGGER.error("Config line has invalid key \"{}\": \"{}\".", tokens[0], line);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Config line has invalid value \"{}\" in \"{}\": {}", tokens[2], line, e.getCause());
        }
    }

    private static void deserialiseStringConfig(String line) {
        String[] tokens = line.strip().split("\\s+", 3);
        if (tokens.length < 2) {
            LOGGER.error("Config line has too few tokens: \"{}\".", line);
            return;
        }

        if (!Objects.equals(tokens[1], "=")) {
            LOGGER.error("Expected \"=\", got \"{}\": \"{}\".", tokens[1], line);
            return;
        }

        String id = tokens[0].substring(MESSAGE_ID_PREFIX.length());

        if (tokens.length == 2) ServerConfig.clearMessage(id);
        else ServerConfig.setMessage(id, tokens[2]);
    }
}
