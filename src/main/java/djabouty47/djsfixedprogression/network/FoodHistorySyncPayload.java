package djabouty47.djsfixedprogression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record FoodHistorySyncPayload(List<String> history) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull FoodHistorySyncPayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("djsfixedprogression:food_history_sync"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull FoodHistorySyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FoodHistorySyncPayload::history,
            FoodHistorySyncPayload::new
    );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
