package cn.meowsmp.carpetmeowaddition.mixin.rule;

import cn.meowsmp.carpetmeowaddition.command.PlayerManagerCommand;
import cn.meowsmp.carpetmeowaddition.command.RegisterCarpetCommands;
import cn.meowsmp.carpetmeowaddition.command.UuidCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void register(CommandManager.RegistrationEnvironment environment, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
        RegisterCarpetCommands.registerCarpetCommands(dispatcher, environment, commandRegistryAccess);
    }
}
