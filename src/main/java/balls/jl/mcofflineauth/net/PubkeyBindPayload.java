package balls.jl.mcofflineauth.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static balls.jl.mcofflineauth.Constants.*;

/**
 * Tells the server to bind the given public key with the user.
 * */
public class PubkeyBindPayload implements CustomPayload  {
    public static final CustomPayload.Id<PubkeyBindPayload> ID = new CustomPayload.Id<>(PUBKEY_BIND_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, PubkeyBindPayload> CODEC = PacketCodec.of(PubkeyBindPayload::write, PubkeyBindPayload::new);

    public String user;
    public PublicKey publicKey;

    public PubkeyBindPayload(String user, PublicKey publicKey) {
        this.user = user;
        this.publicKey = publicKey;
    }

    public PubkeyBindPayload(RegistryByteBuf buf) {
        try {
            ByteBuf keyBuf = buf.readBytes(PUBKEY_SIZE);
            byte[] keyBytes = new byte[PUBKEY_SIZE];
            for (int i = 0; i < PUBKEY_SIZE; ++i)
                keyBytes[i] = keyBuf.readByte();

            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            publicKey = kf.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        user = buf.readString();
    }

    public void write(RegistryByteBuf buf) {
        byte[] keyBytes = publicKey.getEncoded();
        assert (keyBytes.length == PUBKEY_SIZE);
        buf.writeBytes(keyBytes);
        buf.writeString(user);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
