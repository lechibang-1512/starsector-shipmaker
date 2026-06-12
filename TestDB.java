import java.sql.*;

public class TestDB {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:/media/lechibang/WORK1/projects/starsector-shipmaker/ship_editor_database.sqlite";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT entity_type, COUNT(*) FROM indexed_files GROUP BY entity_type;");
            while (rs.next()) {
                System.out.println(rs.getString(1) + ": " + rs.getInt(2));
            }
        }
    }
}
