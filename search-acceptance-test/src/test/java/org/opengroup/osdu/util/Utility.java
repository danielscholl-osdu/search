package org.opengroup.osdu.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class Utility {

    public static String beautifyJsonString(String payload){
        JsonParser jsonParser = new JsonParser();
        if(payload!=null){
            JsonElement jsonElement = jsonParser.parse(payload);
            String beautifiedJson = new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement);
            return beautifiedJson;
        }
        return payload;
    }

    public static boolean containsField(Map<String, Object> record, String fieldName) {
        if(fieldName == null || fieldName.isBlank() || CollectionUtils.isEmpty(record)){
            return false;
        }

        if(fieldName.startsWith("data.")) {
            fieldName = fieldName.substring("data.".length());
            Map<String, Object> data = (Map<String, Object>) record.getOrDefault("data", new HashMap<>());
            if(CollectionUtils.isEmpty(data)) {
                return false;
            }
            return data.containsKey(fieldName);
        }
        else {
            return record.containsKey(fieldName);
        }
    }
}
