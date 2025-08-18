package com.alpaca.utils;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Utility class for loading environment variables from a `.env` file into JVM system properties.
 *
 * <p>This class uses the `dotenv-java` library to read key-value pairs from a `.env` file and
 * transparently inject them into the runtime as system properties via {@link System#setProperty}.
 *
 * <p>This approach supports use cases where environment variables need to be accessible through
 * {@link System#getProperty} instead of {@link System#getenv}, enabling smoother integration with
 * libraries or frameworks that rely on system properties.
 *
 * @see io.github.cdimascio.dotenv.Dotenv
 */
public class LoadEnv {

    /**
     * Loads all entries from the `.env` file into JVM system properties. If a property already
     * exists in the system, it will not be overwritten by default, preserving environment-based
     * configurations over file-based ones.
     *
     * <p>This method should be invoked early in application startup to ensure configuration values
     * are available for the rest of the runtime.
     */
    public static void init() {
        Dotenv.load()
                .entries()
                .forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }
}
