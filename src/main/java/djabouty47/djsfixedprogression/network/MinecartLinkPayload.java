package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record MinecartLinkPayload(int entityId, int linkAId, int linkBId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull MinecartLinkPayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("djsfixedprogression:minecart_link_sync"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull MinecartLinkPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MinecartLinkPayload::entityId,
            ByteBufCodecs.INT, MinecartLinkPayload::linkAId,
            ByteBufCodecs.INT, MinecartLinkPayload::linkBId,
            MinecartLinkPayload::new
    );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
