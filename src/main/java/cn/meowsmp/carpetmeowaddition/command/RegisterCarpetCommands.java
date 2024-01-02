package cn.meowsmp.carpetmeowaddition.command;

import carpet.CarpetServer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RegisterCarpetCommands {
    public static void registerCarpetCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                              @SuppressWarnings("unused") CommandManager.RegistrationEnvironment environment,
                                              CommandRegistryAccess commandBuildContext) {
        if (CarpetServer.settingsManager != null) {
            CarpetServer.settingsManager.registerCommand(dispatcher, commandBuildContext);
            CarpetServer.extensions.forEach((e) -> {
                carpet.api.settings.SettingsManager sm = e.extensionSettingsManager();
                if (sm != null) {
                    sm.registerCommand(dispatcher, commandBuildContext);
                }
            });
        }

        UuidCommand.register(dispatcher);
        PlayerManagerCommand.register(dispatcher);

        CarpetServer.extensions.forEach((e) -> e.registerCommands(dispatcher, commandBuildContext));
    }
}