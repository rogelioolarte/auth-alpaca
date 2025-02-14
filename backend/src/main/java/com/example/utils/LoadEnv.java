package com.example.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class LoadEnv {
    public static void init() {
        Dotenv.load().entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue()));
    }
}
