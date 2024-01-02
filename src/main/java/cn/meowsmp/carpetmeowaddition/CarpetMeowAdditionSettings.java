package cn.meowsmp.carpetmeowaddition;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public class CarpetMeowAdditionSettings {
    private CarpetMeowAdditionSettings() {
        // 禁止外部类创建本类的对象
    }

    public static final String MEOW = "Meow";

    // 简易更新跳略器
    @Rule(
            categories = {MEOW, RuleCategory.SURVIVAL}
    )
    public static boolean simpleUpdateSkipper = false;

    // 易碎深板岩
    @Rule(
            categories = {MEOW, RuleCategory.SURVIVAL}
    )
    public static boolean softDeepslate = false;

    // 获取uuid
    @Rule(
            categories = {MEOW, RuleCategory.SURVIVAL},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandUuid = "ops";

    // 玩家管理器命令
    @Rule(
            categories = {MEOW, RuleCategory.SURVIVAL},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandPlayerManager = "ops";
}
