package cn.meowsmp.carpetmeowaddition.command;

import carpet.utils.CommandHelper;
import cn.meowsmp.carpetmeowaddition.CarpetMeowAdditionSettings;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

public class UuidCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("uuid")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetMeowAdditionSettings.commandUuid))
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("target", EntityArgumentType.entities())
                                .executes(UuidCommand::getUuid)))
                .then(CommandManager.literal("playerUuid")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(UuidCommand::getPlayerUuid))));
    }

    private static int getUuid(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<? extends Entity> target = EntityArgumentType.getEntities(context, "target");
        for (Entity entity : target) {
            String uuid = entity.getUuid().toString();
            MutableText text = getMutableText(uuid);
            MutableText result = entity.getName().copy().append(Text.literal(" -> ")).append(text);
            source.sendMessage(result);
        }
        return target.size();
    }

    private static int getPlayerUuid(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        MinecraftServer server = source.getServer();
        GameProfile gameProfile = Objects.requireNonNull(server.getUserCache()).findByName(name).orElse(null);
        if (gameProfile == null) {
            source.sendMessage(Text.literal("找不到" + name + "的配置文件"));
            return 0;
        }
        MutableText text = getMutableText(gameProfile.getId().toString());
        source.sendMessage(Text.literal(name).append(Text.literal(" -> ").append(text)));
        return 1;
    }

    @NotNull
    private static MutableText getMutableText(String uuid) {
        MutableText text = Text.literal(uuid);
        text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid))
                .withColor(Formatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.copy.click"))));
        return text;
    }
}
