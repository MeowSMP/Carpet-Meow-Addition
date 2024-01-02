package cn.meowsmp.carpetmeowaddition.mixin.rule.carpet;

import carpet.helpers.EntityPlayerActionPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayerActionPack.Action.class)
public interface ActionAccessor {
    @Accessor(value = "isContinuous", remap = false)
    boolean isContinuous();
}
