
package OnlineBanking;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The central data management hub for the Revolutionary Bank application.
 * This class serves as a "Virtual Database" that manages user credentials and 
 * account data. It provides static utility methods for authentication, account 
 * discovery, and persistent storage using Java Object Serialization.
 * 
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 2.0
 * 
 */
public class UserStore {
        
    /** Primary data structure mapping unique usernames to their corresponding BankAccount models. */
    private static Map<String, BankAccount> users = new HashMap<>();
    
    /** Secondary mapping of usernames to plain-text passwords for authentication purposes. */
    private static Map<String, String> passwords = new HashMap<>();
    
    /** The filename used for the serialized database file stored on the local disk. */
    private static final String DATA_FILE = "user_data.ser";

    /**
     * Static initializer block.
     * Automatically executes when the application starts to load existing data 
     * from disk and ensures a default administrator account is available.
     */
    static {
        // Load existing users from the .ser file
        loadData();
        
        // Ensure a system administrator exists for testing and maintenance
        addUser("admin", "password", new BankAccount("8888-1234", "Gabriel", 5000.00, "What is the name of your bank?", "Revolutionary"));
    }

    /**
     * Registers a new user into the system and persists the change to the disk.
     * * @param username The unique identifier chosen by the user.
     * @param password The secret key chosen by the user.
     * @param account The BankAccount object associated with this user identity.
     */
    public static void addUser(String username, String password, BankAccount account) {
        String normalizedUser = username.toLowerCase();
        users.put(normalizedUser, account);
        passwords.put(normalizedUser, password);
        
        // Immediate persistence to prevent data loss
        saveData();
    }

    /**
     * Authenticates a user based on their username and password.
     * * @param username The identifier provided at the login screen.
     * @param password The secret key provided at the login screen.
     * @return The authenticated BankAccount object if credentials match; null otherwise.
     */
    public static BankAccount authenticate(String username, String password) {
        String normalizedUser = username.toLowerCase();
        String storedPassword = passwords.get(normalizedUser);

        // Security check: verification of existence and matching password
        if (storedPassword != null && storedPassword.equals(password)) {
            return users.get(normalizedUser);
        }
        return null;
    }

    /**
     * Checks if a specific username is already taken within the system.
     * * @param username The identifier to check.
     * @return true if the username is found; false otherwise.
     */
    public static boolean userExists(String username) {
        return users.containsKey(username);
    }
    
    /**
     * Serializes the current 'users' and 'passwords' maps to the local filesystem.
     * This ensures that account data persists even after the application is closed.
     */
    public static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
            oos.writeObject(passwords);
        
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }
    
    /**
     * Deserializes account and credential data from the 'user_data.ser' file.
     * Restores the application state to its previous session.
     */
    @SuppressWarnings("unchecked")
    private static void loadData() {
        File file = new File(DATA_FILE);
        
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            users = (Map<String, BankAccount>) ois.readObject();
            passwords = (Map<String, String>) ois.readObject();
        
        } catch (Exception e) {
            System.err.println("Error loading user data: " + e.getMessage());
        }
    }
    
    /**
     * Scans the database for an account matching a specific account number.
     * Critical for processing transfers where the sender only knows the recipient's account number.
     * * @param accountNumber The unique ID to search for (e.g., "1000-xxxx").
     * @return The matching BankAccount object, or null if no match exists.
     */
    public static BankAccount findAccountByNumber(String accountNumber) {
        for (BankAccount account : users.values()) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null; // No account found with that number
    }
    
    /**
     * Updates the password for a specific user and saves the change to disk.
     * * @param username The account identifier to update.
     * @param newPassword The new secret key to store.
     */
    public static void resetPassword(String username, String newPassword) {
        if (passwords.containsKey(username)) {
            passwords.put(username, newPassword);
            saveData(); // Save the new password to user_data.ser
        }
    }
    
    /**
     * Retrieves the security question associated with a username.
     * Used during the password recovery flow.
     * * @param username The identifier to look up.
     * @return The security question string, or null if user does not exist.
     */
    public static String getQuestionForUser(String username) {
        BankAccount account = users.get(username);
        return (account != null) ? account.getSecurityQuestion() : null;
    }
    
    /**
    * Retrieves a BankAccount object based on the provided username identifier.
    * * @param username The unique username key.
    * @return The associated BankAccount model.
    */
   public static BankAccount findAccountByUsername(String username) {
       // This looks into our Map where the Key is the String (username) 
       // and the Value is the BankAccount object.
       return users.get(username.toLowerCase());
   }

}
