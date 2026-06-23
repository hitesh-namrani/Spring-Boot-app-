import java.util.Scanner;

public class Client {
    int id;
    String username;
    String password;
    double balance;

    public Client(int id, String username, String password, double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

    public void display() {
        System.out.println("\nClient Details");
        System.out.println("ID: " + id);
        System.out.println("Username: " + username);
        System.out.println("Balance: " + balance);
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // ID Validation
        int id;
        while (true) {
            System.out.print("Enter 5-digit ID: ");
            id = sc.nextInt();

            if (id >= 10000 && id <= 99999) {
                break;
            }
            System.out.println("Invalid ID! ID must contain exactly 5 digits.");
        }

        sc.nextLine(); // consume newline

        // Username Validation
        String username;
        while (true) {
            System.out.print("Enter Username (must contain a special character): ");
            username = sc.nextLine();

            if (username.matches(".*[^a-zA-Z0-9].*")) {
                break;
            }
            System.out.println("Invalid Username! Include at least one special character.");
        }

        // Password Validation
        String password;
        while (true) {
            System.out.print("Enter Password (max 8 characters or number): ");
            password = sc.nextLine();

            if (password.length() <= 8) {
                break;
            }
            System.out.println("Invalid Password! Password cannot exceed 8 characters.");
        }

        // Balance
        System.out.print("Enter Balance: ");
        double balance = sc.nextDouble();

        Client client = new Client(id, username, password, balance);

        client.display();

        sc.close();
    }
}
