package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record SleepVotePayload() implements CustomPacketPayload {
    public static final Type<@NotNull SleepVotePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "sleep_vote"));
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SleepVotePayload> CODEC = StreamCodec.unit(new SleepVotePayload());

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
