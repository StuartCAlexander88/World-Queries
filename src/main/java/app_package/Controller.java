package app_package;

import java.io.*;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.sql.DriverManager;

public class Controller {

    //---Helpers---
    private static String envOr(String key, String fallback) {

        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    // Find the directory that contains docker-compose.yml (or .yaml)
    private static File locateComposeDir() {
        java.nio.file.Path p = java.nio.file.Paths.get("").toAbsolutePath();
        while (p != null) {
            java.nio.file.Path yml  = p.resolve("docker-compose.yml");
            java.nio.file.Path yaml = p.resolve("docker-compose.yaml");
            if (java.nio.file.Files.isRegularFile(yml) || java.nio.file.Files.isRegularFile(yaml)) {
                return p.toFile();
            }
            p = p.getParent();
        }
        // Fallback: current working dir
        return new File(System.getProperty("user.dir"));
    }

    private static int runCommand(List<String> cmd, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (workDir != null) pb.directory(workDir);
            pb.inheritIO(); // shows compose output in console
            Process p = pb.start();
            return p.waitFor();
        } catch (Exception e) {
            System.err.println("⚠️ Failed to run command: " + String.join(" ", cmd) + " -> " + e.getMessage());
            return -1;
        }
    }

    private static void startDockerCompose(){
        // where docker-compose.yml lives (default: current working dir)
        File composeDir = locateComposeDir();
        System.out.println("Using compose dir: " + composeDir.getAbsolutePath());

        //Try `docker compose up -d`
        List<String> cmd = new ArrayList<>();
        cmd.add("docker"); cmd.add("compose"); cmd.add("up"); cmd.add("-d");
        int code = runCommand(cmd, composeDir);

        if (code != 0){

            // Fallback: `docker-compose up -d`
            System.out.println("↩️ Falling back to `docker-compose up -d` …");
            cmd.clear();
            cmd.add("docker-compose"); cmd.add("up"); cmd.add("-d");
            code = runCommand(cmd, composeDir);
        }

        if (code != 0) {
            System.err.println("❌ Could not start docker compose. Ensure Docker Desktop/Engine is running and docker is on PATH.");
        } else {
            System.out.println("✅ docker compose started (or already running).");
        }
    }

    private static Connection waitForDb(String url, String user, String pass, int maxTries, long sleepMS) {
        SQLException last = null;

        for (int i = 1; i <= maxTries; i++) {
            try {
                Connection c = DriverManager.getConnection(url, user, pass);
                System.out.println("✅ Connected on try " + i);
                return c;
            } catch (SQLException e) {
                last = e;
                System.out.printf("⏳ DB not ready (try %d/%d): %s%n", i, maxTries, e.getMessage());
                try {
                    Thread.sleep(sleepMS);
                } catch (InterruptedException ignored) {
                }
            }
        }
        System.err.println("❌ DB still not ready after retries: " + (last != null ? last.getMessage() : "unknown"));
        return null;
    }

    public static void main(String[] args) {
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        // 1) Always try to bring the stack up
        startDockerCompose();

        // 2) Build connection params (env overrides; defaults match your compose)
        String host = envOr("DB_HOST", "localhost");  // inside compose you'd set DB_HOST=db
        String port = envOr("DB_PORT", "3306");
        String db   = envOr("DB_NAME", "SET08103");
        String user = envOr("DB_USER", "WedTeam1");
        String pass = envOr("DB_PASS", "reallySecurePassword!");

        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                , host, port, db
        );
        System.out.printf("🔌 JDBC URL: %s%n", url.replace(pass, "******"));

        // 3) Wait for MySQL and then query
        try (Connection conn = waitForDb(url, user, pass, 30, Duration.ofSeconds(2).toMillis())) {
            if (conn == null) return; // already logged

            try (Statement stmt = conn.createStatement();
                 // Use backticks or no quotes in MySQL
                 ResultSet rs = stmt.executeQuery("SELECT * FROM `country`")) {

                while (rs.next()) {
                    System.out.println("Row: " + rs.getString(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Unexpected SQL error: " + e.getMessage());
        }


    }
}
