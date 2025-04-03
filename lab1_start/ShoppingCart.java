import backEnd.*;
import java.util.Scanner;

public class ShoppingCart {
    private static boolean safeMode;

    private static void print(Wallet wallet, Pocket pocket) throws Exception {
        System.out.println("Your current balance is: " + wallet.getBalance() + " credits.");
        System.out.println(Store.asString());
        System.out.println("Your current pocket is:\n" + pocket.getPocket());
    }

    private static String scan(Scanner scanner) throws Exception {
        System.out.print("What do you want to buy? (type quit to stop) ");
        return scanner.nextLine();
    }

    public static void main(String[] args) throws Exception {
        safeMode = args.length > 0 && args[0].equalsIgnoreCase("safe");

        Wallet wallet = new Wallet();
        Pocket pocket = new Pocket();
        Scanner scanner = new Scanner(System.in);

        print(wallet, pocket);
        String product = scan(scanner);

        while(!product.equals("quit")) {
            int price = Store.getProductPrice(product);

            if (safeMode) {
                // SAFE MODE: Use atomic safeWithdraw method
                if (wallet.safeWithdraw(price)) {
                    pocket.addProduct(product);
                } else {
                    System.exit(0);
                }
            } else {
                // UNSAFE MODE: Vulnerable to race conditions
                int balance = wallet.getBalance();
                if (balance < price) {
                    System.exit(0);
                } else {
                wallet.setBalance(balance - price);
                pocket.addProduct(product);
                }
            }

            // Just to print everything again...
            print(wallet, pocket);
            product = scan(scanner);
        }
    }
}
