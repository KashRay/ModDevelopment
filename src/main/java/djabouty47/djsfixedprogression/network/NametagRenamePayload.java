package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record NametagRenamePayload(String name) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull NametagRenamePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "rename_nametag"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull NametagRenamePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NametagRenamePayload::name,
            NametagRenamePayload::new
    );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
