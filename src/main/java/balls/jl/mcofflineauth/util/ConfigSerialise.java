package balls.jl.mcofflineauth.util;

import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ConfigSerialise {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static String serialise() {
        StringBuilder sb = new StringBuilder();
        if (ServerConfig.isEnforcing())
            sb.append("enforcing = true\n");
        else
            sb.append("enforcing = false\n");

        if (ServerConfig.allowsUnboundUsers())
            sb.append("allow_unbound_users = true\n");
        else
            sb.append("allow_unbound_users = false\n");

        return sb.toString();
    }

    public static void deserialise(String contents) {
        contents.lines().forEach(line -> {
            if (line.startsWith("#"))
                return;

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
                return;
            }
        });
    }
}
