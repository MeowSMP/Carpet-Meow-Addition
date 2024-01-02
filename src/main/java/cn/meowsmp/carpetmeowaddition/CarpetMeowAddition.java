package cn.meowsmp.carpetmeowaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import cn.meowsmp.carpetmeowaddition.translation.Translations;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CarpetMeowAddition implements ModInitializer, CarpetExtension {
    static {
        CarpetServer.manageExtension(new CarpetMeowAddition());
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("CarpetMeowAddition");

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
