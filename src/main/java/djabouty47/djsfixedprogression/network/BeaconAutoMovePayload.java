package djabouty47.djsfixedprogression.network;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record BeaconAutoMovePayload(String nodeId) implements CustomPacketPayload {
    public static final Type<@NotNull BeaconAutoMovePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(DJsFixedProgression.MOD_ID, "beacon_auto_move"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull BeaconAutoMovePayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.nodeId()),
            buf -> new BeaconAutoMovePayload(buf.readUtf())
    );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
