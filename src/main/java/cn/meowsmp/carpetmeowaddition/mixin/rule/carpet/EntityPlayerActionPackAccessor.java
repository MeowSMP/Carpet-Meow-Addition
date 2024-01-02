package cn.meowsmp.carpetmeowaddition.mixin.rule.carpet;

import carpet.helpers.EntityPlayerActionPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityPlayerActionPack.class)
public interface EntityPlayerActionPackAccessor {
    @Accessor(remap = false)
    Map<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> getActions();
}
