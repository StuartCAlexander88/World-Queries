package app_package;

import java.sql.*;

public class Controller {

    public static void main(String[] args) {
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        String url = "jdbc:mysql://" + System.getenv("DB_HOST") + ":" + System.getenv("DB_PORT") + "/" + System.getenv("DB_NAME");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("✅ Connected to MySQL!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM 'country'");

            while (rs.next()) {
                System.out.println("Row: " + rs.getString(1)); // Adjust column index or name
            }

        } catch (SQLException e) {
            System.err.println("❌ DB connection failed: " + e.getMessage());
        }


    }
}
