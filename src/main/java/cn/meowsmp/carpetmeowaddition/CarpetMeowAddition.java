package cn.meowsmp.carpetmeowaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import cn.meowsmp.carpetmeowaddition.translation.Translations;
import net.fabricmc.api.ModInitializer;

import java.util.Map;

public class CarpetMeowAddition implements ModInitializer, CarpetExtension {
    static {
        CarpetServer.manageExtension(new CarpetMeowAddition());
    }

    @Override
    public void onInitialize() {

    }

    // 在游戏开始时
    @Override
    public void onGameStarted() {
        // 解析Carpet设置
        CarpetServer.settingsManager.parseSettingsClass(CarpetMeowAdditionSettings.class);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translations.getTranslate();
    }
}
