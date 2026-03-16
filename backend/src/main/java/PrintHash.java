import com.vaultx.util.SecurityUtil;

public class PrintHash {
    public static void main(String[] args) {
        System.out.println(SecurityUtil.hashPassword("User@123"));
    }
}
