package org.honeycomb.tools.netty.utils;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;

/**
 * User: luluful
 * Date: 4/8/19
 * Time: 3:15 PM
 */
public class GsonUtil {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    public static <T> T fromJson(String jsonString, Type type) {
        return GSON.fromJson(jsonString, type);
    }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static JsonObject toJsonObject(String jsonString){
        return new JsonParser().parse(jsonString).getAsJsonObject();
    }
}
