
package OnlineBanking;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The central data management hub for the Revolutionary Bank application.
 * This class facilitates all interactions between the Java application and the 
 * MySQL database. It handles security-critical operations including password 
 * verification, ACID-compliant financial transfers, and transaction auditing.
 * 
 * 
 * @author Gabriel J. Zayas
 * Date: 4/07/2026
 * @version 3.0
 * 
 */
public class UserStore {
    
    /**
     * Registers a new user in the MySQL database.
     * 
     * * LOGIC & DECISIONS:
     * 1. Uses a PreparedStatement to prevent SQL Injection.
     * 2. Requests 'RETURN_GENERATED_KEYS' to retrieve the auto-incremented Primary Key.
     * 3. DECISION: If 'affectedRows' is 0, the insertion failed (e.g., connection drop), 
     * and an exception is thrown to prevent partial data states.
     * 
     * @param username Unique identifier for login.
     * @param name The user's full legal name.
     * @param password The SHA-256 hashed password.
     * @param accountNum The generated "1000-xxxx" account number.
     * @param balance The starting account balance.
     * @param question The custom security recovery question.
     * @param answer The hashed answer to the recovery question.
     * @return The new user's auto-incremented ID; returns -1 on failure.
     */
    public static int addUserToDatabase(String username, 
                                        String name, 
                                        String password, 
                                        String accountNum, 
                                        double balance, 
                                        String question, 
                                        String answer) {

        String query = "INSERT INTO users (username, full_name, password, account_number, balance, security_question, security_answer) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setString(3, password);
            pstmt.setString(4, accountNum);
            pstmt.setDouble(5, balance);
            pstmt.setString(6, question);
            pstmt.setString(7, answer);
            
            // EXECUTE the insertion FIRST
            int affectedRows = pstmt.executeUpdate();

            // DECISION: Verify the row was actually created before attempting to fetch ID
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            
            // Returns the user DB record int ID
            try (var generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Return the actual ID
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if something went wrong
    }

    /**
     * Authenticates a user by matching hashed credentials against the database.
     * 
     * * VARIABLES & LOGIC:
     * - 'hashedPassword': The user's input is immediately hashed to match stored values.
     * - 'ResultSet rs': Holds the row data if a match is found.
     * - DECISION: If rs.next() is true, a new BankAccount object is instantiated 
     * using the mapped database columns.
     * 
     * @param username The identifier provided at login.
     * @param password The raw text password (hashed internally).
     * @return An authenticated BankAccount object, or null if credentials fail.
     */
   public static BankAccount authenticate(String username, String password) {
       // Hash the input password before checking the database
       String hashedPassword = PasswordUtil.hashPassword(password);

       // SQL query to find the user
       String query = "SELECT * FROM users WHERE username = ? AND password = ?";

       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query)) {

           pstmt.setString(1, username);
           pstmt.setString(2, hashedPassword);

           try (ResultSet rs = pstmt.executeQuery()) {
               if (rs.next()) {
                   // Map the database columns to the BankAccount constructor
                   return new BankAccount(
                       rs.getInt("id"),
                       rs.getString("account_number"),
                       rs.getString("full_name"), 
                       rs.getDouble("balance"),
                       rs.getString("security_question"),
                       rs.getString("security_answer")
                   );
               }
           }
       } catch (SQLException e) {
           System.err.println("Database Authentication Error: " + e.getMessage());
       }

       return null; // No match found or connection failed
   }

    /**
     * Checks if a username already exists to prevent duplicate registrations.
     * @param username The identifier to verify.
     * @return true if the username is found (case-insensitive).
     */
    public static boolean userExists(String username) {
        String query = "SELECT 1 FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username.toLowerCase());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Returns true if a row is found
            }
        
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Scans the database for an account matching a specific account number.
     * Critical for processing transfers where the sender only knows the recipient's account number.
     * 
     * @param accountNumber The unique ID to search for (e.g., "1000-xxxx").
     * @return The matching BankAccount object, or null if no match exists.
     */
    public static BankAccount findAccountByNumber(String accountNumber) {
        
        String query = "SELECT * FROM users WHERE account_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, accountNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    
                    return new BankAccount(
                        rs.getInt("id"),
                        rs.getString("account_number"),
                        rs.getString("full_name"),
                        rs.getDouble("balance"),
                        rs.getString("security_question"),
                        rs.getString("security_answer")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Updates the password hash for a user.
     * @param username The user's login name.
     * @param hashedPassword The pre-hashed new password string.
     */
    public static void resetPassword(String username, String hashedPassword) {
        String query = "UPDATE users SET password = ? WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setString(1, hashedPassword);
        pstmt.setString(2, username.toLowerCase());
        pstmt.executeUpdate();
        
        } catch (SQLException e) {
            System.err.println("Database error during password reset: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves the security question associated with a username.
     * Used during the password recovery flow.
     * 
     * @param username The identifier to look up.
     * @return The security question string, or null if user does not exist.
     */
    public static String getQuestionForUser(String username) {
        String query = "SELECT security_question FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username.toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("security_question");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching security question: " + e.getMessage());
        }
        return null;
    }
    
    /**
    * Retrieves a BankAccount object based on the provided username identifier.
    * 
    * @param username The unique username key.
    * @return The associated BankAccount model, or null if not found.
    */
    public static BankAccount findAccountByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username.toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new BankAccount(
                        rs.getInt("id"),
                        rs.getString("account_number"),
                        rs.getString("full_name"),
                        rs.getDouble("balance"),
                        rs.getString("security_question"),
                        rs.getString("security_answer")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding account by username: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the balance of a single user account in the database.
     * * LOGIC & DECISION:
     * - Uses a PreparedStatement to securely map the new numerical balance to the 
     * unique account number string.
     * - This method is typically called for non-transfer actions, such as 
     * basic deposits or withdrawals.
     * 
     * @param accountNumber The unique "1000-xxxx" identifier for the account.
     * @param newBalance The finalized numerical total to be saved to the database.
     */
    public static void updateBalance(String accountNumber, double newBalance) {
        String query = "UPDATE users SET balance = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, accountNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Executes a secure, atomic P2P (Peer-to-Peer) transfer between two users.
     * * TRANSACTION LOGIC (ACID Compliance):
     * - DECISION: 'setAutoCommit(false)' is called to treat the two separate updates 
     * (debit and credit) as a single unit of work.
     * - COMMIT: If both updates execute without error, 'conn.commit()' is called 
     * to persist the changes.
     * - ROLLBACK: If any SQLException occurs (e.g., database timeout or constraint 
     * violation), 'conn.rollback()' is executed to ensure neither account balance 
     * is altered partially.
     * 
     * @param senderAcc The account number of the user sending the funds.
     * @param senderBal The new calculated balance for the sender after the deduction.
     * @param recipAcc The account number of the user receiving the funds.
     * @param recipBal The new calculated balance for the recipient after the addition.
     * @return true if the transaction was committed successfully; false if it was rolled back.
     */
    public static boolean executeTransfer(String senderAcc, double senderBal, String recipAcc, double recipBal) {
        String updateSender = "UPDATE users SET balance = ? WHERE account_number = ?";
        String updateRecip = "UPDATE users SET balance = ? WHERE account_number = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement pstmt1 = conn.prepareStatement(updateSender);
                 PreparedStatement pstmt2 = conn.prepareStatement(updateRecip)) {

                pstmt1.setDouble(1, senderBal);
                pstmt1.setString(2, senderAcc);
                pstmt1.executeUpdate();

                pstmt2.setDouble(1, recipBal);
                pstmt2.setString(2, recipAcc);
                pstmt2.executeUpdate();

                conn.commit(); // Save changes
                return true;
            } catch (SQLException e) {
                conn.rollback(); // Undo changes if something goes wrong
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Logs a specific transaction event into the audit table.
     * 
     * * VARIABLES:
     * @param userId The primary key of the acting user.
     * @param t The Transaction object containing details (amount, note, etc).
     */
    public static void logTransaction(int userId, Transaction t) {
        // Generate a unique ID for this specific transaction
        String transactionId = "TR-" + UUID.randomUUID().toString().substring(0, 8);

        String query = "INSERT INTO transactions (transaction_id, user_id, description, type, amount, note) " +
                       "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, transactionId);  
            pstmt.setInt(2, userId);             
            pstmt.setString(3, t.getDescription());
            pstmt.setString(4, t.getType());
            pstmt.setDouble(5, t.getAmount());
            pstmt.setString(6, t.getNote());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL ERROR in logTransaction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetches the full history of transactions for a specific user.
     * * LOGIC & LOOP:
     * - The SQL query uses 'ORDER BY transaction_date DESC' to ensure the UI displays 
     * the most recent activity first.
     * - LOOP: A 'while (rs.next())' loop iterates through the ResultSet, mapping 
     * database rows to Transaction objects until no rows remain.
     * 
     * @param userId The Primary Key 'id' used to filter the transactions table.
     * @return A List of Transaction objects; empty list if no transactions exist.
     */
    public static List<Transaction> loadTransactionHistory(int userId) {
       List<Transaction> history = new ArrayList<>();
       // Order by date descending so the newest transactions appear at the top
       String query = "SELECT * FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC";

       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query)) {

           pstmt.setInt(1, userId);

           try (ResultSet rs = pstmt.executeQuery()) {
               while (rs.next()) {
                   // Use the second constructor we added to the Transaction class
                   Transaction t = new Transaction(
                       rs.getTimestamp("transaction_date").toLocalDateTime(),
                       rs.getString("description"),
                       rs.getString("type"),
                       rs.getDouble("amount"),
                       rs.getString("note")
                   );
                   history.add(t);
               }
           }
       } catch (SQLException e) {
           System.err.println("Error loading transaction history: " + e.getMessage());
       }
       return history;
    }
   
    /**
     * Records the very first deposit for a newly created account in the audit table.
     * 
     * VARIABLES:
     * @param userId The Primary Key of the user making the deposit.
     * @param amount The initial numerical value being added to the account.
     */
    public static void logInitialDeposit(int userId, double amount) {
        // Generate a unique ID (e.g., TR-12345678)
        String transactionId = "TR-" + System.currentTimeMillis();
        
        String query = "INSERT INTO transactions (transaction_id, user_id, description, type, amount, note) " +
                   "VALUES (?, ?, ?, ?, ?, ?)";

        // Use your existing DatabaseConnection utility
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, transactionId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, "Initial Deposit");
            pstmt.setString(4, "DEPOSIT");
            pstmt.setDouble(5, amount);
            pstmt.setString(6, "Account Deposit");
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Database error logging initial deposit: " + e.getMessage());
        }
    }
    
    /** * Updates the user's display name in the 'users' table.
     * * DECISION:
     * - Returns the result of 'executeUpdate() > 0', which effectively acts as a 
     * boolean check to see if the ID was valid and the row was modified.
     * 
     * @param userId The unique ID of the user to update.
     * @param newName The updated full name string.
     * @return true if the record was successfully updated; false otherwise.
     */
    public static boolean updateUserName(int userId, String newName) {
        String query = "UPDATE users SET full_name = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, newName);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** * Updates the account recovery data for a user.
     * 
     * @param userId The unique ID of the user.
     * @param question The updated security question text.
     * @param answer The updated security answer (should be pre-processed or hashed).
     * @return true if the security info was updated successfully.
     */
    public static boolean updateSecurityInfo(int userId, String question, String answer) {
        String query = "UPDATE users SET security_question = ?, security_answer = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, question);
            pstmt.setString(2, answer);
            pstmt.setInt(3, userId);
            
            return pstmt.executeUpdate() > 0;
        
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** * Updates the user's password hash in the database.
     * * LOGIC:
     * - Calls 'PasswordUtil.hashPassword' to ensure the 'rawNewPassword' is never 
     * stored as plain text, maintaining system security.
     * 
     * @param userId The unique ID of the user.
     * @param rawNewPassword The new plain-text password to be hashed.
     * @return true if the password update was successful.
     */
    public static boolean updatePassword(int userId, String rawNewPassword) {
        String hashedPassword = PasswordUtil.hashPassword(rawNewPassword);
        String query = "UPDATE users SET password = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** * Compares a provided password against the one stored in the database.
     * * DECISION:
     * - Uses '.equals()' to compare the newly hashed input with the database result.
     * - If 'rs.next()' is false, the method defaults to false as the user ID is invalid.
     * 
     * @param userId The unique ID of the user being verified.
     * @param passwordToCheck The raw password input from the user.
     * @return true if the hashes match; false if they differ or user is not found.
     */
    public static boolean verifyPassword(int userId, String passwordToCheck) {
        String hashedPassword = PasswordUtil.hashPassword(passwordToCheck);
        String query = "SELECT password FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    
                    return rs.getString("password").equals(hashedPassword);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /** * Retrieves a full BankAccount data model based on the Primary Key.
     * * LOGIC:
     * - Maps SQL 'ResultSet' columns directly to the 'BankAccount' constructor 
     * parameters for consistent object instantiation.
     * 
     * @param userId The Primary Key 'id' to search for.
     * @return A populated BankAccount object if found; null if no user matches the ID.
     */
    public static BankAccount findAccountById(int userId) {
       String query = "SELECT * FROM users WHERE id = ?";
       
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query)) {

           pstmt.setInt(1, userId);

           try (ResultSet rs = pstmt.executeQuery()) {
               if (rs.next()) {
                   return new BankAccount(
                       rs.getInt("id"),
                       rs.getString("account_number"),
                       rs.getString("full_name"),
                       rs.getDouble("balance"),
                       rs.getString("security_question"),
                       rs.getString("security_answer")
                   );
               }
           }
       } catch (SQLException e) {
           System.err.println("Error finding account by ID: " + e.getMessage());
       }
       return null;
    }
   
    /** * Permanently removes a user record from the database.
     * 
     * @param userId The Primary Key of the user to be deleted.
     * @return true if a row was actually removed; false if the ID did not exist.
     */
    public static boolean deleteUser(int userId) {
        String query = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
