package cn.meowsmp.carpetmeowaddition;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public class CarpetMeowAdditionSettings {
    private CarpetMeowAdditionSettings() {
        // 禁止外部类创建本类的对象
    }

    public static final String MEOW = "Meow";

    //测试
    @Rule(
            categories = {MEOW, RuleCategory.SURVIVAL}
    )
    public static boolean test = false;
}
