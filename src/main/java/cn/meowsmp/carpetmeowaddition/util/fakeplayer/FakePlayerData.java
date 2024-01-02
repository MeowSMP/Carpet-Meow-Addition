package cn.meowsmp.carpetmeowaddition.util.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.patches.EntityPlayerMPFake;
import cn.meowsmp.carpetmeowaddition.CarpetMeowAddition;
import cn.meowsmp.carpetmeowaddition.mixin.rule.carpet.ActionAccessor;
import cn.meowsmp.carpetmeowaddition.mixin.rule.carpet.EntityPlayerActionPackAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class FakePlayerData {
    public static final String EXTENSION = ".json";
    /**
     * 假玩家名
     */
    @NotNull
    private final String name;

    /**
     * 假玩家的别名
     */
    @Nullable
    private final String alias;

    /**
     * 该对象的创建者的玩家名
     */
    @Nullable
    private final String creator;

    /**
     * 假玩家位置
     */
    private final double[] playerPos = new double[3];
    /**
     * 假玩家的朝向
     */
    private final double[] direction = new double[2];

    /**
     * 假玩家是否潜行
     */
    private final boolean sneaking;
    /**
     * 假玩家所在的维度
     */
    @NotNull
    private final String dimension;
    /**
     * 假玩家的游戏模式
     */
    @Nullable
    private final String gamemode;

    private final EntityPlayerActionPack playerActionPack;

    public FakePlayerData(EntityPlayerMPFake fakePlayer, @Nullable String alias, CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        this.name = fakePlayer.getName().getString();
        this.alias = alias;
        // 创建者
        if (player == null) {
            this.creator = null;
        } else {
            this.creator = player.getName().getString();
        }
        // 玩家的位置
        this.playerPos[0] = fakePlayer.getX();
        this.playerPos[1] = fakePlayer.getY();
        this.playerPos[2] = fakePlayer.getZ();
        // 玩家的朝向
        this.direction[0] = fakePlayer.getYaw();
        this.direction[1] = fakePlayer.getPitch();
        // 假玩家是否潜行
        this.sneaking = fakePlayer.isSneaking();
        // 假玩家的维度
        this.dimension = fakePlayer.getWorld().getDimensionKey().getValue().toString();
        // 假玩家的游戏模式
        this.gamemode = fakePlayer.interactionManager.getGameMode().getName();
        // 假玩家的动作
        this.playerActionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
    }

    /**
     * 保存假玩家的数据
     *
     * @return 是否为重新保存（覆盖）
     */
    public static boolean save(FakePlayerData fakePlayerData, MinecraftServer server) throws IOException {
        JsonObject jsonProperty = new JsonObject();
        jsonProperty.addProperty("name", fakePlayerData.name);
        jsonProperty.addProperty("alias", fakePlayerData.alias);
        jsonProperty.addProperty("creator", fakePlayerData.creator);
        // 添加假玩家坐标属性
        JsonObject jsonPlayerPos = new JsonObject();
        jsonPlayerPos.addProperty("x", fakePlayerData.playerPos[0]);
        jsonPlayerPos.addProperty("y", fakePlayerData.playerPos[1]);
        jsonPlayerPos.addProperty("z", fakePlayerData.playerPos[2]);
        jsonProperty.add("playerPos", jsonPlayerPos);
        // 添加假玩家朝向属性
        JsonObject jsonDirection = new JsonObject();
        jsonDirection.addProperty("yaw", fakePlayerData.direction[0]);
        jsonDirection.addProperty("pitch", fakePlayerData.direction[1]);
        jsonProperty.add("direction", jsonDirection);
        jsonProperty.addProperty("sneaking", fakePlayerData.sneaking);
        jsonProperty.addProperty("dimension", fakePlayerData.dimension);
        jsonProperty.addProperty("gamemode", fakePlayerData.gamemode);
        JsonObject jsonAction = new JsonObject();
        // 添加假玩家的动作信息
        EntityPlayerActionPackAccessor actionPack = (EntityPlayerActionPackAccessor) fakePlayerData.playerActionPack;
        Map<ActionType, Action> actions = actionPack.getActions();
        // 左键
        Action attack = actions.get(ActionType.ATTACK);
        JsonObject jsonAttack = new JsonObject();
        if (attack != null && !attack.done) {
            jsonAttack.addProperty("interval", attack.interval);
            jsonAttack.addProperty("continuous", ((ActionAccessor) attack).isContinuous());
            jsonAction.add("attack", jsonAttack);
        }
        // 右键
        Action use = actions.get(ActionType.USE);
        JsonObject jsonUse = new JsonObject();
        if (use != null && !use.done) {
            jsonUse.addProperty("interval", use.interval);
            jsonUse.addProperty("continuous", ((ActionAccessor) use).isContinuous());
            jsonAction.add("use", jsonUse);
        }
        jsonProperty.add("action", jsonAction);

        // 将假玩家数据以UTF-8编码写入本地文件
        File file = new File(getFile(server), fakePlayerData.name + FakePlayerData.EXTENSION);
        boolean exists = file.exists();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            bw.write(gson.toJson(jsonProperty));
        }
        return exists;
    }

    // 加载假玩家
    public static File load(String playerName, CommandContext<ServerCommandSource> context, boolean spawn) {
        MinecraftServer server = context.getSource().getServer();
        File[] files = getFile(server).listFiles();
        if (files != null) {
            for (File file : files) {
                // 如果文件扩展名不是.json，直接跳过
                if (!file.getName().endsWith(FakePlayerData.EXTENSION)) {
                    continue;
                }
                BufferedReader readJson;
                try {
                    readJson = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
                    try (readJson) {
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(readJson, JsonObject.class);
                        // 判断假玩家的名称或别名是否与文件中的匹配
                        if (isDesignatedFile(playerName, jsonObject)) {
                            if (spawn) {
                                // 生成假玩家
                                createFake(jsonObject, context);
                            }
                            return file;
                        }
                    }
                } catch (IOException e) {
                    CarpetMeowAddition.LOGGER.warn("处理名为" + file.getName() + "的文件时出现意外问题");
                }

            }
        }
        server.getPlayerManager().broadcast(Text.literal("找不到名为" + playerName + "的玩家信息"), false);
        return null;
    }

    // 判断当前文件是否为保存了指定玩家信息的文件
    private static boolean isDesignatedFile(String playerName, JsonObject jsonObject) {
        return (jsonObject.has("alias") && Objects.equals(playerName, jsonObject.get("alias").getAsString())) ||
                (jsonObject.has("name") && Objects.equals(playerName, jsonObject.get("name").getAsString()));
    }

    // 创造假玩家
    private static void createFake(JsonObject jsonObject, CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        // 假玩家名
        String username = jsonObject.get("name").getAsString();
        if (server.getPlayerManager().getPlayer(username) != null) {
            // 玩家已存在
            context.getSource().sendMessage(Text.literal("玩家" + username + "已存在"));
            return;
        }
        // 假玩家的位置
        JsonObject playerPos = jsonObject.get("playerPos").getAsJsonObject();
        Vec3d pos = new Vec3d(playerPos.get("x").getAsDouble(), playerPos.get("y").getAsDouble(), playerPos.get("z").getAsDouble());
        JsonObject playerDirection = jsonObject.get("direction").getAsJsonObject();
        // 生成假玩家
        EntityPlayerMPFake fakePlayer = EntityPlayerMPFake.createFake(username, server, pos,
                playerDirection.get("yaw").getAsDouble(), playerDirection.get("pitch").getAsDouble(),
                getWorld(jsonObject.get("dimension").getAsString()),
                getGameMode(jsonObject.get("gamemode").getAsString()), false);
        // 设置假玩家潜行
        fakePlayer.setSneaking(jsonObject.get("sneaking").getAsBoolean());
        EntityPlayerActionPack action = ((ServerPlayerInterface) fakePlayer).getActionPack();
        if (jsonObject.has("action")) {
            JsonObject jsonAction = jsonObject.get("action").getAsJsonObject();
            // 设置假玩家左键
            if (jsonAction.has("attack")) {
                JsonObject attack = jsonAction.get("attack").getAsJsonObject();
                if (attack.get("continuous").getAsBoolean()) {
                    // 左键长按
                    action.start(ActionType.ATTACK, Action.continuous());
                } else {
                    // 间隔左键
                    int interval = attack.get("interval").getAsInt();
                    action.start(ActionType.ATTACK, Action.interval(interval));
                }
            }
            // 设置假玩家右键
            if (jsonAction.has("use")) {
                JsonObject attack = jsonAction.get("use").getAsJsonObject();
                if (attack.get("continuous").getAsBoolean()) {
                    // 右键长按
                    action.start(ActionType.USE, Action.continuous());
                } else {
                    // 间隔右键
                    int interval = attack.get("interval").getAsInt();
                    action.start(ActionType.USE, Action.interval(interval));
                }
            }
        }
    }

    // 删除假玩家数据
    public static void deleteFile(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "name");
        File[] files = getFile(context).listFiles();
        if (files != null) {
            for (File file : files) {
                try (BufferedReader jsonReader = new BufferedReader(new FileReader(file))) {
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                    if (isDesignatedFile(playerName, jsonObject)) {
                        // 删除文件前关闭io流
                        jsonReader.close();
                        boolean deleted = file.delete();
                        context.getSource().sendMessage(Text.literal((deleted ? "已" : "无法") + "删除" + playerName + "的玩家数据"));
                        return;
                    }
                } catch (FileNotFoundException e) {
                    CarpetMeowAddition.LOGGER.warn("找不到名为" + file.getName() + "的文件");
                } catch (IOException e) {
                    CarpetMeowAddition.LOGGER.warn("处理文件" + file.getName() + "时出现意外问题");
                }
            }
        }
        context.getSource().sendMessage(Text.literal("找不到名为" + playerName + "的玩家信息"));
    }

    // 在命令中列出建议
    public static SuggestionProvider<ServerCommandSource> listSuggests() {
        return (context, builder)
                -> CommandSource.suggestMatching(Arrays.stream(Objects.requireNonNull(getFile(context.getSource().getServer()).listFiles()))
                .map(file -> {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        try (br) {
                            Gson gson = new Gson();
                            JsonObject jsonObject = gson.fromJson(br, JsonObject.class);
                            if (jsonObject.has("alias")) {
                                return jsonObject.get("alias").getAsString();
                            } else if (jsonObject.has("name")) {
                                return jsonObject.get("name").getAsString();
                            }
                        }
                    } catch (IOException e) {
                        CarpetMeowAddition.LOGGER.error("处理" + file.getName() + "文件时出现意外问题：", e);
                    }
                    return "";
                }).map(StringArgumentType::escapeIfRequired), builder);
    }

    // 显示假玩家数据的详细信息
    public static void showInfo(String username, CommandContext<ServerCommandSource> context) {
        File[] files = getFile(context).listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().endsWith(FakePlayerData.EXTENSION)) {
                    continue;
                }
                try {
                    BufferedReader jsonReader = new BufferedReader(new FileReader(file));
                    try (jsonReader) {
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                        if (isDesignatedFile(username, jsonObject)) {
                            showInfo(context, jsonObject);
                            return;
                        }
                    } catch (IOException e) {
                        CarpetMeowAddition.LOGGER.warn("处理文件" + file.getName() + "时出现意外问题");
                    }
                } catch (FileNotFoundException e) {
                    CarpetMeowAddition.LOGGER.warn("找不到名为" + file.getName() + "的文件");
                }
            }
        }
        context.getSource().sendMessage(Text.literal("找不到玩家" + username + "的数据信息"));
    }

    private static void showInfo(CommandContext<ServerCommandSource> context, JsonObject jsonObject) {
        // 一行代码实现功能，一百行代码防小天才，每个元素都判断是否存在
        ArrayList<MutableText> list = new ArrayList<>();
        // 假玩家名
        if (jsonObject.has("name")) {
            list.add(Text.literal("名称：" + jsonObject.get("name").getAsString()));
        }
        // 假玩家别名
        if (jsonObject.has("alias")) {
            list.add(Text.literal("别名：" + jsonObject.get("alias").getAsString()));
        }
        // 创建者玩家名
        if (jsonObject.has("creator")) {
            list.add(Text.literal("创建者：" + jsonObject.get("creator").getAsString()));
        }
        // 假玩家位置
        if (jsonObject.has("playerPos")) {
            JsonObject jsonPos = jsonObject.get("playerPos").getAsJsonObject();
            if (jsonPos.has("x") && jsonPos.has("y") && jsonPos.has("z")) {
                list.add(Text.literal("位置：["
                        + String.format("%.2f", jsonPos.get("x").getAsDouble()) + ", "
                        + String.format("%.2f", jsonPos.get("x").getAsDouble()) + ", "
                        + String.format("%.2f", jsonPos.get("x").getAsDouble()) + "]"
                ));
            }
        }
        // 假玩家朝向
        if (jsonObject.has("direction")) {
            JsonObject jsonPos = jsonObject.get("direction").getAsJsonObject();
            if (jsonPos.has("yaw") && jsonPos.has("pitch")) {
                list.add(Text.literal("朝向："
                        + String.format("%.2f", jsonPos.get("yaw").getAsDouble()) + ", "
                        + String.format("%.2f", jsonPos.get("pitch").getAsDouble())
                ));
            }
        }
        // 假玩家是否潜行
        if (jsonObject.has("sneaking")) {
            list.add(Text.literal("潜行：" + jsonObject.get("sneaking").getAsBoolean()));
        }
        // 假玩家所在维度
        if (jsonObject.has("dimension")) {
            String dimensionId = jsonObject.get("dimension").getAsString();
            switch (dimensionId) {
                case "minecraft:overworld", "overworld" -> list.add(Text.literal("维度：主世界"));
                case "minecraft:the_nether", "the_nether" -> list.add(Text.literal("维度：下界"));
                case "minecraft:the_end", "the_end" -> list.add(Text.literal("维度：末地"));
                default -> list.add(Text.literal("维度：" + dimensionId));
            }
        }
        // 假玩家游戏模式
        if (jsonObject.has("gamemode")) {
            String gameMode = jsonObject.get("gamemode").getAsString();
            switch (gameMode) {
                case "survival" -> list.add(Text.literal("游戏模式：生存模式"));
                case "creative" -> list.add(Text.literal("游戏模式：创造模式 "));
                case "adventure" -> list.add(Text.literal("游戏模式：冒险模式"));
                case "spectator" -> list.add(Text.literal("游戏模式：旁观模式"));
                default -> list.add(Text.literal("游戏模式：" + gameMode));
            }
        }
        // 假玩家动作
        if (jsonObject.has("action")) {
            JsonObject action = jsonObject.get("action").getAsJsonObject();
            list.add(Text.literal("动作："));
            // 左键
            if (action.has("attack")) {
                JsonObject attack = action.get("attack").getAsJsonObject();
                if (attack.has("continuous") && attack.get("continuous").getAsBoolean()) {
                    list.add(Text.literal("    左键：长按"));
                } else if (attack.has("interval")) {
                    list.add(Text.literal("    左键：间隔" + attack.get("interval").getAsInt() + "gt"));
                }
            }
            // 右键
            if (action.has("use")) {
                JsonObject use = action.get("use").getAsJsonObject();
                if (use.has("continuous") && use.get("continuous").getAsBoolean()) {
                    list.add(Text.literal("    右键：长按"));
                } else if (use.has("interval")) {
                    list.add(Text.literal("    右键：间隔" + use.get("interval").getAsInt() + "gt"));
                }
            }
        }
        // （假玩家数据的）注释
        if (jsonObject.has("annotation")) {
            list.add(Text.literal("注释：" + jsonObject.get("annotation").getAsString()));
        }
        // 发送消息
        for (MutableText text : list) {
            context.getSource().sendMessage(text);
        }
    }

    public static void list(CommandContext<ServerCommandSource> context) {
        File[] files = getFile(context).listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().endsWith(FakePlayerData.EXTENSION)) {
                    continue;
                }
                try {
                    BufferedReader jsonReader = new BufferedReader(new FileReader(file));
                    try (jsonReader) {
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                        eachPlayerData(context, jsonObject);
                    } catch (IOException e) {
                        CarpetMeowAddition.LOGGER.warn("处理文件" + file.getName() + "时出现意外问题");
                    }
                } catch (FileNotFoundException e) {
                    CarpetMeowAddition.LOGGER.warn("找不到名为" + file.getName() + "的文件");
                }
            }
        }
    }

    private static void eachPlayerData(CommandContext<ServerCommandSource> context, JsonObject jsonObject) {
        ArrayList<Object> list = new ArrayList<>();
        // 假玩家别名
        String playerAlias = null;
        // 假玩家名
        String playerName = null;
        // 为假玩家名别名赋值
        if (jsonObject.has("alias")) {
            playerAlias = jsonObject.get("alias").getAsString();
        }
        // 为假玩家本命赋值
        if (jsonObject.has("name")) {
            playerName = jsonObject.get("name").getAsString();
        }
        if (playerAlias == null && playerName == null) {
            return;
        }
        // 定义假玩家登录和下线的命令
        String loggedOutCommand = "/player " + playerName + " kill";
        String loggedInCommand = "/playerManager spawn " + (playerAlias == null ? playerName :
                StringArgumentType.escapeIfRequired(playerAlias));
        // 点击上线
        MutableText loggedIn = Text.literal("[↑]").styled(style -> style
                // 点击执行命令
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, loggedInCommand))
                // 添加显示文本
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击上线")))
                // 设置为绿色
                .withColor(Formatting.GREEN));
        list.add(loggedIn);
        list.add(" ");
        // 点击下线
        MutableText loggedOut = Text.literal("[↓]").styled(style -> style
                // 点击执行命令
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, loggedOutCommand))
                // 添加显示文本
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击下线")))
                // 设置为红色
                .withColor(Formatting.RED));
        list.add(loggedOut);
        list.add(" ");
        // 在假玩家名上添加注释，如果没有注释，但是有别名，则添加本命
        MutableText name = Text.literal(playerAlias == null ? playerName : playerAlias);
        if (jsonObject.has("annotation")) {
            name.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.literal(jsonObject.get("annotation").getAsString()))));
        } else if (playerAlias != null) {
            MutableText nameText = Text.literal(playerName);
            name.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    nameText)));
        }
        list.add(name);
        context.getSource().sendMessage(appendAll(list));
    }

    private static MutableText appendAll(ArrayList<Object> list) {
        MutableText mutableText = Text.literal("");
        for (Object object : list) {
            if (object instanceof String str) {
                mutableText.append(Text.literal(str));
            } else if (object instanceof Text text) {
                mutableText.append(text);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return mutableText;
    }


    /**
     * 将字符串解析为世界ID
     *
     * @param worldId 世界的ID，如果指定了命名空间，则使用指定的，否则使用minecraft
     * @return 世界类型的注册表项
     */
    private static RegistryKey<World> getWorld(String worldId) {
        if (worldId.contains(":")) {
            String[] split = worldId.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException();
            }
            return RegistryKey.of(RegistryKeys.WORLD, new Identifier(split[0], split[1]));
        }
        return RegistryKey.of(RegistryKeys.WORLD, new Identifier(worldId));
    }

    /**
     * 将字符串解析为游戏模式，如果无法解析，默认为生存模式
     *
     * @param gameMode 游戏模式的字符串
     * @return 字符串指定的游戏模式
     */
    private static GameMode getGameMode(String gameMode) {
        return GameMode.byName(gameMode);
    }

    // 获取文件
    private static File getFile(CommandContext<ServerCommandSource> context) {
        return getFile(context.getSource().getServer());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File getFile(MinecraftServer server) {
        File file = server.getSavePath(WorldSavePath.ROOT).resolve("carpetmeowaddition")
                .resolve("fake_player_data").toFile();
        if (file.exists()) {
            return file;
        }
        file.mkdirs();
        return file;
    }
}
