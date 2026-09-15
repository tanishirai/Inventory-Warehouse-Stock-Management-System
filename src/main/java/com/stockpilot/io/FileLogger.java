package com.stockpilot.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Appends timestamped events to logs/app.log. Demonstrates classic
 * character-stream I/O (FileWriter/PrintWriter) with try-with-resources
 * so each write opens, flushes, and closes cleanly — safe to call from
 * multiple threads because the method itself is synchronized.
 */
public final class FileLogger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + "/app.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileLogger() { }

    public static synchronized void log(String message) {
        try {
            Files.createDirectories(Path.of(LOG_DIR));
            try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
                out.printf("[%s] %s%n", LocalDateTime.now().format(FMT), message);
            }
        } catch (IOException e) {
            // Logging must never crash the app; surface to stderr instead.
            System.err.println("FileLogger: failed to write log entry: " + e.getMessage());
        }
    }
}
