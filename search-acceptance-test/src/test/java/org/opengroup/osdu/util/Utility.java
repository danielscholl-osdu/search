package org.opengroup.osdu.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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
}
