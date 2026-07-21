package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TemplateStructurePiece.class)
public class TemplateStructurePieceMixin {
    @Shadow protected StructurePlaceSettings placeSettings;

    /**
     * Inject code when generating structure blocks, swapping all crying obsidian with normal obsidian.
     */
    @Inject(method = "postProcess", at = @At("HEAD"))
    private void removeCryingObsidian(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos, CallbackInfo ci) {
        //Check if the structure piece generating is a ruined portal
        if ((Object) this instanceof RuinedPortalPiece) {

            //Inject a rule processor to dynamically swap all crying obsidian for obsidian
            this.placeSettings.addProcessor(new RuleProcessor(List.of(
                    new ProcessorRule(
                            new BlockMatchTest(Blocks.CRYING_OBSIDIAN),
                            AlwaysTrueTest.INSTANCE,
                            Blocks.OBSIDIAN.defaultBlockState()
                    )
            )));
        }
    }
}
