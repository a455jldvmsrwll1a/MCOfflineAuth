package balls.jl.mcofflineauth.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

import static balls.jl.mcofflineauth.Constants.LOGIN_CHALLENGE_PACKET_ID;

/**
 * Tells the client to sign the given data and reply with the signature.
 */
public class LoginChallengePayload implements CustomPayload {
    public static final CustomPayload.Id<LoginChallengePayload> ID = new CustomPayload.Id<>(LOGIN_CHALLENGE_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, LoginChallengePayload> CODEC = PacketCodec.of(LoginChallengePayload::write, LoginChallengePayload::new);

    public final UUID id;
    public final byte[] data;
    public final String user;

    public LoginChallengePayload(UUID id, byte[] data, String user) {
        this.id = id;
        this.data = data;
        this.user = user;
    }

    public LoginChallengePayload(PacketByteBuf buf) {
        id = buf.readUuid();
        data = buf.readByteArray();
        user = buf.readString();
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(id);
        buf.writeByteArray(data);
        buf.writeString(user);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
