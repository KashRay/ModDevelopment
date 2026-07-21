package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record EnchantmentSelectPayload(Identifier enchantmentId, boolean useToken) implements CustomPacketPayload {

    /**
     * The unique packet ID.
     */
    public static final CustomPacketPayload.Type<@NotNull EnchantmentSelectPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "select_enchantment"));

    /**
     * The Codec tells Minecraft how to turn our data into bytes, and how to read it back out
     */
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull EnchantmentSelectPayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            EnchantmentSelectPayload::enchantmentId,
            ByteBufCodecs.BOOL, EnchantmentSelectPayload::useToken,
            EnchantmentSelectPayload::new
    );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}