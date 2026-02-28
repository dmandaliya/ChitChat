import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

public class LoginService {
    private List<User> allUsers;
    private User empty = new User(); // To check if anyone is logged out.
    private User currentUser = empty;
    private Scanner s = new Scanner(System.in);

    private User user;
    public LoginService(User user) {
        this.user = user;
    }

    // Mainly for debugging
    public void display(){
        System.out.println("Name: " + user.getFname() + " " + user.getLname());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Password: " + user.getPassword());
        System.out.println("Hashed Password: " + user.getHashedPassword());
    }

    // This format is for terminal, will change to use for GUI later.
    public void initialize() {
        System.out.print("First name: ");
        user.setFname(s.next());
        System.out.print("Last name: ");
        user.setLname(s.next());
        System.out.print("Username: ");
        user.setUsername(s.next());
        System.out.print("Password: ");
        user.setPassword(s.next());
        System.out.println("\n");
        user.setNewAccount(false);
        display();
    }

    public void login() {
        System.out.println(user.getNewAccount());
        if ((currentUser == empty) && (!user.getNewAccount())) display();
        else if ((currentUser == empty) && (user.getNewAccount())) initialize();
        else {
            System.out.println("User is currently logged in.");
            return;
        }
        System.out.println("Logged in: " + user.getUsername() + "\n");
        currentUser = user;
    }

    public void logout() {
        currentUser = empty;
        System.out.println("Logged out: " + user.getUsername() + "\n");
    }
}
