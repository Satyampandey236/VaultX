import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InsertJohn {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/vaultx_db", "root", "satyam@12345");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO users (full_name, email, phone, password_hash, role, is_active) VALUES ('John Doe', 'john@example.com', '1234567890', '3e7c19576488862816f13b512cacf3e4ba97dd97243ea0bd6a2ad1642d86ba72', 'USER', TRUE)");
            System.out.println("John Doe inserted!");
            conn.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
