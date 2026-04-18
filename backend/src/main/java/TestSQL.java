import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TestSQL {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/vaultx_db", "root", "satyam@12345");
            Statement stmt = conn.createStatement();
            String sql = "INSERT IGNORE INTO accounts (account_number, user_id, account_type, balance) " +
                         "SELECT 'VX10000001', id, 'SAVINGS', 25000.00 " +
                         "FROM users WHERE email = 'john@example.com'";
            int rows = stmt.executeUpdate(sql);
            System.out.println("Rows affected: " + rows);
            conn.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
