package com.jade.es.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 多层嵌套json数据转换为单层，同时规格化
 */
public class JSONUtil {
    public static String jsonFormatter(String uglyJSONString) {
        Map<String, Object> map = new HashMap<>();
        parseJson2Map(map, uglyJSONString, null);
        return JSON.toJSONString(map);
    }

    public static void parseJson2Map(Map map, JSONObject jsonObject, String parentKey) {
        Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
        Iterator<Map.Entry<String, Object>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            Object value = entry.getValue();
            String key = StringUtils.isEmpty(parentKey) ? entry.getKey(): parentKey + "." + entry.getKey();
            if (value instanceof JSONObject) {
                parseJson2Map(map, (JSONObject) value, key);
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                Object[] arrList = array.stream().toArray();
                for (Object arritem : arrList) {
                    parseJson2Map(map, (JSONObject) arritem, key);
                }
            } else {
                map.put(key, value);
            }
        }
    }

    public static void parseJson2Map(Map map, String json, String parentKey) {
        JSONObject jsonObject = JSON.parseObject(json);
        parseJson2Map(map, jsonObject, parentKey);
    }
}
