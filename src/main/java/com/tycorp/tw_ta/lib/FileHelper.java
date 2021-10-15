package com.tycorp.tw_ta.lib;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileHelper {

    public static void appendToFile(String fname, String line) throws IOException {
        try {
            Files.write(Paths.get(fname), (line + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            throw e;
        }
    }

    public static void writeToFile(String filename, String content) throws IOException {
        try {
            FileWriter fWriter = new FileWriter(filename);
            fWriter.write(content);
            fWriter.close();
        } catch (IOException e) {
            throw e;
        }
    }

}
