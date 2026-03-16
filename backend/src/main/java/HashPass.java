import com.vaultx.util.SecurityUtil;

public class HashPass {
    public static void main(String[] args) {
        System.out.println("Admin@123 -> " + SecurityUtil.hashPassword("Admin@123"));
        System.out.println("User@123 -> " + SecurityUtil.hashPassword("User@123"));
        System.out.println("admin123 -> " + SecurityUtil.hashPassword("admin123"));
    }
}
