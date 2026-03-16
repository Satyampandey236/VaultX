import com.vaultx.service.BankService;
import com.vaultx.model.User;

public class TestLogin {
    public static void main(String[] args) {
        try {
            BankService service = new BankService();
            User u = service.login("admin@vaultx.com", "admin123");
            if (u != null) {
                System.out.println("Login Success: " + u.getFullName());
            } else {
                System.out.println("Login Failed: null returned");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
