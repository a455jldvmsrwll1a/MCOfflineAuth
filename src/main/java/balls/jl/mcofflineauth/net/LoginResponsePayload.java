package balls.jl.mcofflineauth.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

import static balls.jl.mcofflineauth.Constants.*;

/**
 * Client's response to the server, providing the signature for the challenge data.
 * */
public class LoginResponsePayload implements CustomPayload {
    public static final CustomPayload.Id<LoginResponsePayload> ID = new CustomPayload.Id<>(LOGIN_RESPONSE_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, LoginResponsePayload> CODEC = PacketCodec.of(LoginResponsePayload::write, LoginResponsePayload::new);

    public UUID id;
    public String user;
    public byte[] signature;

    public LoginResponsePayload(UUID id, String user, byte[] signature) {
        this.id = id;
        this.signature = signature;
        this.user = user;
    }

    public LoginResponsePayload(PacketByteBuf buf) {
        id = buf.readUuid();
        user = buf.readString();

        int length = buf.readableBytes();
        signature = new byte[length];

        for (int i = 0; i < length; ++i)
            signature[i] = buf.readByte();
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(id);
        buf.writeString(user);
        buf.writeBytes(signature);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
