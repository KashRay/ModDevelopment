package djabouty47.djsfixedprogression.network;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record EnchantingAutoMovePayload(Identifier enchantmentId, boolean useToken) implements CustomPacketPayload {
    public static final Type<@NotNull EnchantingAutoMovePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(DJsFixedProgression.MOD_ID, "enchanting_auto_move"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull EnchantingAutoMovePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeIdentifier(payload.enchantmentId());
                buf.writeBoolean(payload.useToken());
            },
            buf -> new EnchantingAutoMovePayload(buf.readIdentifier(), buf.readBoolean())
    );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
