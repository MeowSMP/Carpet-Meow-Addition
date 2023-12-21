package cn.meowsmp.carpetmeowaddition.translation;

import carpet.CarpetSettings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Translations {
    public static final HashMap<String, Map<String, String>> LANG = new HashMap<>();

    //获取翻译
    public static Map<String, String> getTranslate() {
        String language = CarpetSettings.language;
        if (LANG.containsKey(language)) {
            // 如果集合中有需要的元素，直接从集合中获取，而不是再从文件读取一遍
            return LANG.get(language);
        }
        String dataJSON;
        try {
            dataJSON = IOUtils.toString(
                    Objects.requireNonNull(
                            Translations.class
                                    .getClassLoader()
                                    .getResourceAsStream(String.format("assets/carpet-meow-addition/lang/" + language + ".json"))
                    ),
                    // 以UTF-8编码读取翻译文件
                    StandardCharsets.UTF_8
            );
        } catch (NullPointerException | IOException e) {
            try {
                dataJSON = IOUtils.toString(
                        Objects.requireNonNull(
                                Translations.class
                                        .getClassLoader()
                                        .getResourceAsStream("assets/carpet-meow-addition/lang/en_us.json")
                        ),
                        StandardCharsets.UTF_8
                );
            } catch (NullPointerException | IOException ex) {
                return null;
            }
        }
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        Map<String, String> lang = gson.fromJson(dataJSON, new TypeToken<Map<String, String>>() {
        }.getType());
        // 将翻译文件添加到集合，下次获取时，直接从文件获取
        LANG.put(language, lang);
        return lang;
    }
}
