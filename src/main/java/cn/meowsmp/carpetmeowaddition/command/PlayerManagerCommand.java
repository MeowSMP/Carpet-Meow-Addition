package cn.meowsmp.carpetmeowaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import cn.meowsmp.carpetmeowaddition.CarpetMeowAdditionSettings;
import cn.meowsmp.carpetmeowaddition.util.fakeplayer.FakePlayerData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PlayerManagerCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("playerManager")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetMeowAdditionSettings.commandPlayerManager))
                .then(CommandManager.literal("save")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> save(context, null))
                                .then(CommandManager.argument("alias", StringArgumentType.string())
                                        .executes(context -> save(context, StringArgumentType.getString(context, "alias"))))))
                .then(CommandManager.literal("spawn")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(FakePlayerData.listSuggests())
                                .executes(PlayerManagerCommand::spawn)))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(FakePlayerData.listSuggests())
                                .executes(PlayerManagerCommand::delete)))
                .then(CommandManager.literal("supplement")
                        .then(CommandManager.literal("annotation")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(FakePlayerData.listSuggests())
                                        .then(CommandManager.argument("annotation", StringArgumentType.string())
                                                .executes(PlayerManagerCommand::addAnnotation))))
                        .then(CommandManager.literal("alias")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(FakePlayerData.listSuggests())
                                        .then(CommandManager.argument("alias", StringArgumentType.string())
                                                .executes(PlayerManagerCommand::addAlias)))))

                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("info", StringArgumentType.string())
                                .suggests(FakePlayerData.listSuggests())
                                .executes(PlayerManagerCommand::showInfo)))
                .then(CommandManager.literal("list")
                        .executes(PlayerManagerCommand::list))
        );
    }


    // 保存假玩家数据
    @SuppressWarnings("SpellCheckingInspection")
    private static int save(CommandContext<ServerCommandSource> context, @Nullable String alias) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        if (player instanceof EntityPlayerMPFake fakePlayer) {
            FakePlayerData fakePlayerData = new FakePlayerData(fakePlayer, alias, context);
            try {
                boolean resave = FakePlayerData.save(fakePlayerData, context.getSource().getServer());
                context.getSource().sendMessage(Text.literal("已" + (resave ? "重新保存" : "保存")
                        + (alias == null ? player.getName().getString() : alias) + "的玩家数据"));
                return 1;
            } catch (IOException e) {
                context.getSource().sendMessage(Text.literal("无法保存假玩家数据"));
            }
        } else {
            context.getSource().sendMessage(Text.literal(player.getName().getString() + "不是假玩家"));
        }
        return 0;
    }

    // 生成假玩家
    private static int spawn(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        try {
            FakePlayerData.load(name, context, true);
            return 1;
        } catch (NullPointerException e) {
            context.getSource().getServer().getPlayerManager().broadcast(Text.literal("无法从文件读取假玩家数据"), false);
        }
        return 0;
    }

    // 删除假玩家数据
    private static int delete(CommandContext<ServerCommandSource> context) {
        FakePlayerData.deleteFile(context);
        return 1;
    }

    // 修改json文件
    private static boolean modifyJsonFile(File file, String property, String value) throws IOException {
        boolean hasProperty;
        String json;
        BufferedReader jsonReader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
        // 读取旧的json文件的内容
        try (jsonReader) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
            hasProperty = jsonObject.has(property);
            // 为json文件添加新值
            jsonObject.addProperty(property, value);
            // 将json转化为字符串
            json = gson.toJson(jsonObject);
        }
        // 将修改好的json重新写入本地文件
        BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
        try (jsonWriter) {
            jsonWriter.write(json);
        }
        return hasProperty;
    }

    // 添加注释
    private static int addAnnotation(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        File file = FakePlayerData.load(name, context, false);
        if (file != null) {
            try {
                boolean hasAnnotation = modifyJsonFile(file, "annotation", StringArgumentType.getString(context, "annotation"));
                context.getSource().sendMessage(Text.literal(hasAnnotation ? "修改了" + name + "的注释" : "为" + name + "添加注释"));
                return 1;
            } catch (IOException e) {
                context.getSource().sendMessage(Text.literal("尝试添加注释时出现意外问题"));
            }
        }
        return 0;
    }

    // 添加别名
    private static int addAlias(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        File file = FakePlayerData.load(name, context, false);
        if (file != null) {
            try {
                boolean hasAlias = modifyJsonFile(file, "alias", StringArgumentType.getString(context, "alias"));
                context.getSource().sendMessage(Text.literal(hasAlias ? "修改了" + name + "的别名" : "为" + name + "添加了别名"));
            } catch (IOException e) {
                context.getSource().sendMessage(Text.literal("尝试设置别名时出现意外问题"));
            }
        }
        return 0;
    }

    // 显示玩家数据的信息
    private static int showInfo(CommandContext<ServerCommandSource> context) {
        FakePlayerData.showInfo(StringArgumentType.getString(context, "info"), context);
        return 1;
    }

    // 列出所有玩家数据
    private static int list(CommandContext<ServerCommandSource> context) {
        FakePlayerData.list(context);
        return 1;
    }
}