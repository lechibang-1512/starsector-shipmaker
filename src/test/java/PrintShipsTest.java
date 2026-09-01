import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class PrintShipsTest {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:ship_editor_database.sqlite");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT entity_type, COUNT(*) FROM indexed_files GROUP BY entity_type;");
        while(rs.next()) {
            System.out.println(rs.getString(1) + " : " + rs.getInt(2));
        }
    }
}
