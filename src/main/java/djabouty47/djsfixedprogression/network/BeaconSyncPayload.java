package djabouty47.djsfixedprogression.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record BeaconSyncPayload(CompoundTag upgrades) implements CustomPacketPayload {
    public static final Type<@NotNull BeaconSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("djsfixedprogression", "beacon_sync_payload"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull BeaconSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, BeaconSyncPayload::upgrades,
            BeaconSyncPayload::new
    );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() { return ID; }
}
