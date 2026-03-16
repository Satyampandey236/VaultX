import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdateDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/vaultx_db", "root", "satyam@12345");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE users SET password_hash='240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9' WHERE email='admin@vaultx.com'");
            stmt.executeUpdate("UPDATE users SET password_hash='3e7c19576488862816f13b512cacf3e4ba97dd97243ea0bd6a2ad1642d86ba72' WHERE email='john@example.com'");
            System.out.println("Hashes updated successfully!");
            conn.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
