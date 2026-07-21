package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record SleepVoteSuccessPayload() implements CustomPacketPayload {
    public static final Type<@NotNull SleepVoteSuccessPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "sleep_vote_success"));
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SleepVoteSuccessPayload> CODEC = StreamCodec.unit(new SleepVoteSuccessPayload());

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
