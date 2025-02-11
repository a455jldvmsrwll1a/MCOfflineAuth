package balls.jl.mcofflineauth.net;

import balls.jl.mcofflineauth.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Tells the client to send over its public key.
 */
public class PubkeyQueryPayload implements CustomPayload {
    public static final CustomPayload.Id<PubkeyQueryPayload> ID = new CustomPayload.Id<>(Constants.PUBKEY_QUERY_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, PubkeyQueryPayload> CODEC = PacketCodec.of(PubkeyQueryPayload::write, PubkeyQueryPayload::new);

    public PubkeyQueryPayload() {
    }

    public PubkeyQueryPayload(RegistryByteBuf buf) {
    }

    public void write(RegistryByteBuf buf) {
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
