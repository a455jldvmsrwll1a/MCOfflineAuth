package balls.jl.mcofflineauth.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

import static balls.jl.mcofflineauth.Constants.LOGIN_RESPONSE_PACKET_ID;

/**
 * Client's response to the server, providing the signature for the challenge data.
 */
public class LoginResponsePayload implements CustomPayload {
    public static final CustomPayload.Id<LoginResponsePayload> ID = new CustomPayload.Id<>(LOGIN_RESPONSE_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, LoginResponsePayload> CODEC = PacketCodec.of(LoginResponsePayload::write, LoginResponsePayload::new);

    public final UUID id;
    public final String user;
    public final byte[] signature;

    public LoginResponsePayload(UUID id, String user, byte[] signature) {
        this.id = id;
        this.signature = signature;
        this.user = user;
    }

    public LoginResponsePayload(PacketByteBuf buf) {
        id = buf.readUuid();
        user = buf.readString();
        signature = buf.readByteArray();
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(id);
        buf.writeString(user);
        buf.writeByteArray(signature);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
