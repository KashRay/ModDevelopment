package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record SleepVoteCancelPayload() implements CustomPacketPayload {
    public static final Type<@NotNull SleepVoteCancelPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "sleep_vote_cancel"));
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SleepVoteCancelPayload> CODEC = StreamCodec.unit(new SleepVoteCancelPayload());

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
