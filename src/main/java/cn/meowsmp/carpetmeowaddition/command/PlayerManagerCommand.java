package cn.meowsmp.carpetmeowaddition.command;

import carpet.patches.EntityPlayerMPFake;
import cn.meowsmp.carpetmeowaddition.util.fakeplayer.FakePlayerData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class PlayerManagerCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("playerManager").requires(source -> true)
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> add(context, null))
                                .then(CommandManager.argument("alias", StringArgumentType.string())
                                        .executes(context -> add(context, StringArgumentType.getString(context, "alias"))))))
                .then(CommandManager.literal("spawn")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(FakePlayerData.listSuggests())
                                .executes(PlayerManagerCommand::spawn)))
        );
    }

    private static int add(CommandContext<ServerCommandSource> context, @Nullable String alias) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        if (player instanceof EntityPlayerMPFake fakePlayer) {
            FakePlayerData fakePlayerData = new FakePlayerData(fakePlayer, alias, context);
            try {
                FakePlayerData.save(fakePlayerData, context.getSource().getServer());
                return 1;
            } catch (IOException e) {
                context.getSource().sendMessage(Text.literal("保存json文件时出现意外问题"));
            }
        }
        return 0;
    }

    private static int spawn(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        try {
            FakePlayerData.load(name, context.getSource().getServer());
            return 1;
        } catch (IOException e) {
            context.getSource().getServer().getPlayerManager().broadcast(Text.literal("加载json文件时出现意外问题"), false);
        }
        return 0;
    }
}