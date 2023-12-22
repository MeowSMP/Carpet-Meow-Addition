package cn.meowsmp.carpetmeowaddition.mixin.rule;

import cn.meowsmp.carpetmeowaddition.CarpetMeowAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {
    @WrapOperation(method = "getRenderConnectionType(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;Z)Lnet/minecraft/block/enums/WireConnection;", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"))
    private Block canRunOnTop(BlockState instance, Operation<Block> original) {
        if (CarpetMeowAdditionSettings.simpleUpdateSkipper) {
            return null;
        }
        return original.call(instance);
    }
}
