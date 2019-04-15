package org.honeycomb.tools.netty.autoconfig;


import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import org.honeycomb.tools.netty.utils.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: luluful
 * Date: 4/11/19
 * Time: 8:36 PM
 */
@Component
public class HcEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(HcEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "systemProperties";

    private static final String SERVER_CONFIG = "config";

    /**
     * The default order for the processor.
     */
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private int order = DEFAULT_ORDER;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Map<String, String> env = System.getenv();
        String envHcConfig = env.get("HC_APP_CONFIG");
        Map<String, Object> flatMap = Maps.newConcurrentMap();
        if (StringUtils.isEmpty(envHcConfig)) {
            //本地运行取不到hc的配置，默认设置成true
            flatMap.put("server.port.enable", "true");
        } else {
            JsonObject jsonObject = GsonUtil.toJsonObject(envHcConfig);
            //提取hc参数里server config配置
            JsonObject configObject = jsonObject.getAsJsonObject(SERVER_CONFIG);
            if (configObject == null) {
                logger.error("hc config is not contains {}", SERVER_CONFIG);
                return;
            }
            JsonParser parser = JsonParserFactory.getJsonParser();
            Map<String, Object> configMap = parser.parseMap(GsonUtil.toJson(configObject));
            flatMap = flatten(configMap);
            //远程需要监听sock文件，端口号默认设置程false
            flatMap.put("server.port.enable", "false");
        }
        addOrReplace(environment.getPropertySources(), flatMap);
    }

    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (String key : map.keySet()) {
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, map.get(key));
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }

    private Map<String, Object> flatten(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten(null, result, map);
        return result;
    }

    private void flatten(String prefix, Map<String, Object> result,
                         Map<String, Object> map) {
        String namePrefix = (prefix != null) ? prefix + "." : "";
        map.forEach((key, value) -> extract(namePrefix + key, result, value));
    }

    private void extract(String name, Map<String, Object> result, Object value) {
        if (value instanceof Map) {
            flatten(name, result, (Map<String, Object>) value);
        } else if (value instanceof Collection) {
            int index = 0;
            for (Object object : (Collection<Object>) value) {
                extract(name + "[" + index + "]", result, object);
                index++;
            }
        } else {
            result.put(name, value);
        }
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
