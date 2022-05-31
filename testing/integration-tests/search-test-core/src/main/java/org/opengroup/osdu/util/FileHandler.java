package org.opengroup.osdu.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileHandler {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String readFile(String fileName) throws IOException {
        InputStream inputStream = FileHandler.class.getResourceAsStream(String.format("/testData/%s",fileName));
        if(inputStream == null) {
            throw new IOException();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); 
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toString(StandardCharsets.UTF_8.toString());
    }

    public static <T> T readFile(String fileName, Class<T> targetClass) throws IOException {
        InputStream is = getFileStream(fileName);
        return mapper.readValue(is, targetClass);
    }

    private static InputStream getFileStream(String fileName) {
        return FileHandler.class.getResourceAsStream(String.format("/testData/%s", fileName));
    }
    
}
