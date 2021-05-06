package com.relativerank.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestConstants {

    private TestConstants() {}

    static {
        String json;
        try {
            var path = Paths.get("src/test/resources/onePageMalListTestResponse.json");
            json = Files.readAllLines(path).get(0);
        } catch (IOException e) {
            json = null;
        }

        onePageMalListJsonString = json;
    }

    public static final String onePageMalListJsonString;
}
