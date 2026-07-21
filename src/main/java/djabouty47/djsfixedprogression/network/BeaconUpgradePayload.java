package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record BeaconUpgradePayload(String nodeId) implements CustomPacketPayload {
    public static final Type<@NotNull BeaconUpgradePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "beacon_upgrade_payload"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull BeaconUpgradePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BeaconUpgradePayload::nodeId,
            BeaconUpgradePayload::new
    );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() { return ID; }
}
