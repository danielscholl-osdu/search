// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.smart.attributes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.smart.models.Attribute;

import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import jakarta.servlet.http.HttpServletResponse;

@Log
public class AttributeMappingReader implements AttributesRepository {
    private static final String DEFAULT_ATTRIBUTE_MAPPING_PATH = "attributemapping.json";

    List<Attribute> convertJsonIntoAttributes(String jsonString) {
        List<Attribute> attributes = new ArrayList<>();
        if (jsonString.isEmpty()) return attributes;
        try {
            Gson gson = new Gson();
            String content = getFile(jsonString);
            List<Attribute> lstAttributes = gson.fromJson(content, new TypeToken<List<Attribute>>() {
            }.getType());
            if (lstAttributes != null) {
                return lstAttributes;
            }
        } catch (Exception e) {
            throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Failed to load attribute mapping config file. Please verify path and the structure.", e.getMessage());
        }
        return attributes;
    }


    private String getFile(String fileName) throws UnsupportedEncodingException {

        StringBuilder result = new StringBuilder("");

        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(URLDecoder.decode(classLoader.getResource(fileName).getFile(), "utf-8"));

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Failed to load attribute mapping config file. Please verify path and the structure.", e.getMessage());
        }

        return result.toString();

    }

    @Override
    public List<Attribute> read() {
        return convertJsonIntoAttributes(DEFAULT_ATTRIBUTE_MAPPING_PATH);
    }
}
