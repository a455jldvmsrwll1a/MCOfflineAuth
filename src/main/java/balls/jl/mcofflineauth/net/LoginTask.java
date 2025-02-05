package balls.jl.mcofflineauth.net;

import balls.jl.mcofflineauth.Constants;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;

import java.util.function.Consumer;

public class LoginTask implements ServerPlayerConfigurationTask  {
    @Override
    public void sendPacket(Consumer<Packet<?>> sender) {}

    @Override
    public Key getKey() {
        return new Key(Constants.LOGIN_TASK.toString());
    }
}
