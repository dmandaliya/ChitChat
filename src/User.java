import java.util.ArrayList;
import java.util.List;

public class User {

    // -------- Values --------
    private String Fname; // First name
    private String Lname; // Last name
    private String username;
    private String userPassword; // Password String that user chose
    private String hashedPassword; // Used for lig
    private List<User> friendList = new ArrayList<>(); // Friends list
    private String lastOnline; // If online, it puts time in this variable. When offline it stops updating.
    private PreferenceService preference; // Preference OBJ for each user
    private boolean newAccount = true; // Default true until values are chosen.
    private boolean loggedIn = false;

    public User() {
        // For initializing a user with all empty values.
    }

    // If you want to define a user with a full name instead of all empty values.
    public User (String fname, String lname) {
        this.Fname = fname;
        this.Lname = lname;
    }

    // -------- Set/Get Fname (first name) --------
    public void setFname(String fname) {
        this.Fname = fname;
    }
    public String getFname() {
        return Fname;
    }

    // -------- Set/Get Lname (last name) --------
    public void setLname(String lname) {
        this.Lname = lname;
    }
    public String getLname() {
        return Lname;
    }

    // -------- Set/Get Lname (last name) --------
    public boolean getNewAccount() {return newAccount;}
    public void setNewAccount(boolean newAccount) {this.newAccount = newAccount;}

    // -------- Set/Get username --------
    public void setUsername(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }

    // -------- Set/Get password --------
    public void setPassword(String password) {
        this.userPassword = password;
        this.hashedPassword = EncryptionService.hashPassword(password);
    }
    public String getPassword() {
        return this.userPassword;
    }
    public String getHashedPassword() {
        return this.hashedPassword;
    }

    // -------- Get Friendlist --------
    public List<User> getFriendList() {
        return friendList;
    }

    // -------- Set/Get loggedIn --------
    public boolean getLoggedIn() {return loggedIn;}
    public void setLoggedIn(boolean loggedIn) {this.loggedIn = loggedIn;}

    // -------- Set/Get password --------
    public void setLastOnline(String lastOnline) {
        this.lastOnline = lastOnline;
    }

    public String getLastOnline() {
        return lastOnline;
    }

    // -------- Set/Get preference OBJ --------
    public PreferenceService getPreference() {
        return preference;
    }
    public void setPreference(PreferenceService preference) {
        this.preference = preference;
    }

    public void printList(User u) {
        for (User i: u.getFriendList()) {
            System.out.println(i.getFname() + " " + i.getLname());
        }
    }
}
