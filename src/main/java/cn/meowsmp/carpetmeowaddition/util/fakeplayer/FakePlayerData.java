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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    private String alias;

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
    private boolean sneaking;
    /**
     * 假玩家所在的维度
     */
    @NotNull
    private String dimension;
    /**
     * 假玩家的游戏模式
     */
    @Nullable
    private String gamemode;

    private EntityPlayerActionPack playerActionPack;

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

    public static void save(FakePlayerData fakePlayerData, MinecraftServer server) throws IOException {
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
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(getFile(server),
                fakePlayerData.name + FakePlayerData.EXTENSION), StandardCharsets.UTF_8))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            bw.write(gson.toJson(jsonProperty));
        }
    }

    public static void load(String playerName, MinecraftServer server) throws IOException {
        File[] files = getFile(server).listFiles();
        if (files != null) {
            for (File file : files) {
                BufferedReader readJson = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
                try (readJson) {
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(readJson, JsonObject.class);
                    // 判断假玩家的名称或别名是否与文件中的匹配
                    if ((jsonObject.has("alias") && Objects.equals(playerName, jsonObject.get("alias").getAsString())) ||
                            (jsonObject.has("name") && Objects.equals(playerName, jsonObject.get("name").getAsString()))) {
                        // 生成假玩家
                        createFake(jsonObject, server);
                        return;
                    }
                }
            }
        }
        server.getPlayerManager().broadcast(Text.literal("找不到名为" + playerName + "的玩家信息"), false);
    }

    private static void createFake(JsonObject jsonObject, MinecraftServer server) {
        // 假玩家名
        JsonObject playerPos = jsonObject.get("playerPos").getAsJsonObject();
        // 假玩家的位置
        Vec3d pos = new Vec3d(playerPos.get("x").getAsDouble(), playerPos.get("y").getAsDouble(), playerPos.get("z").getAsDouble());
        JsonObject playerDirection = jsonObject.get("direction").getAsJsonObject();
        // 生成假玩家
        EntityPlayerMPFake fakePlayer = EntityPlayerMPFake.createFake(jsonObject.get("name").getAsString(), server, pos,
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
