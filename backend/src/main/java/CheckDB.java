import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/vaultx_db", "root", "satyam@12345");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, email, password_hash, is_active FROM users");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getString("email") + " | " + rs.getString("password_hash") + " | " + rs.getBoolean("is_active"));
            }
            conn.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
