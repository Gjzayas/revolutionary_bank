
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
 * verification, ACID-compliant financial transfers, transaction auditing, 
 * and Loan processing.
 * 
 * 
 * @author Gabriel J. Zayas
 * Date: 6/22/2026
 * @version 4.0
 * 
 */
public class UserStore {
    
    /**
     * Registers a new user in the MySQL database.
     * 
     * 
     * LOGIC & DECISIONS:
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
     * 
     * VARIABLES & LOGIC:
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
     * 
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
     * 
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
     * 
     * LOGIC & DECISION:
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
     * 
     * TRANSACTION LOGIC (ACID Compliance):
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
     * Saves a new transaction record into the system database.
     * * This method automatically creates a random, unique 8-character tracking ID 
     * prefixed with "TR-L-" (for example: TR-L-A1B2C3D4), maps all the transaction 
     * details like description, type, amount, and notes, and inserts them straight 
     * into the transaction history table.
     * 
     * * @param userId The ID number of the bank user who owns this transaction.
     * @param t      The transaction object containing the amount, type, description, and notes.
     * @param conn   The active database connection being used to run this save operation.
     * @throws SQLException If something goes wrong while communicating with the SQL database.
     */
    public static void logTransaction(int userId, Transaction t, Connection conn) throws SQLException {
        String transactionId = "TR-L-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String query = "INSERT INTO transactions (transaction_id, user_id, description, type, amount, note) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, transactionId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, t.getDescription());
            pstmt.setString(4, t.getType());
            pstmt.setDouble(5, t.getAmount());
            pstmt.setString(6, t.getNote());
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Fetches the full history of transactions for a specific user.
     * 
     * LOGIC & LOOP:
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
        // Generate a unique ID (e.g., TR-INIT-12345678)
        String transactionId = "TR-INIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        String query = "INSERT INTO transactions (transaction_id, user_id, description, type, amount, note) " +
                   "VALUES (?, ?, ?, ?, ?, ?)";

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
    
    /** 
     * Updates the user's display name in the 'users' table.
     * 
     * DECISION:
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

    /** 
     * Updates the account recovery data for a user.
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

    /** 
     * Updates the user's password hash in the database.
     * 
     * LOGIC:
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

    /** 
     * Compares a provided password against the one stored in the database.
     * 
     * DECISION:
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
    
    /** 
     * Retrieves a full BankAccount data model based on the Primary Key.
     * 
     * LOGIC:
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
   
    /** 
     * Permanently removes a user record from the database.
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
    
    /**
     * Records a new loan application and its status determined by the LoanEngine.
     * 
     * LOGIC & DECISIONS:
     * 1. If the loan is 'APPROVED', this method initiates a Transaction (ACID) to 
     *    simultaneously update the user's balance and record the loan.
     * 2. If 'DENIED' or 'PENDING', it simply logs the record for history/review.
     * 
     * @param loan The Loan object containing principal, status, and engine notes.
     * @return true if the database operations were successful.
     */
    public static boolean submitLoanRequest(Loan loan) {
        String loanQuery = "INSERT INTO loans (user_id, principal_amount, interest_rate, term_months, monthly_income_reported, status, status_note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String balanceQuery = "UPDATE users SET balance = balance + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Enable Transaction for Approved loans

            try (PreparedStatement loanPstmt = conn.prepareStatement(loanQuery);
                 PreparedStatement balPstmt = conn.prepareStatement(balanceQuery)) {

                // 1. Insert Loan Record
                loanPstmt.setInt(1, loan.getUserId());
                loanPstmt.setDouble(2, loan.getPrincipalAmount());
                loanPstmt.setDouble(3, loan.getInterestRate());
                loanPstmt.setInt(4, loan.getTermMonths());
                loanPstmt.setDouble(5, loan.getMonthlyIncome());
                loanPstmt.setString(6, loan.getStatus());
                loanPstmt.setString(7, loan.getStatusNote());
                loanPstmt.executeUpdate();

                // 2. If APPROVED, immediately credit the account
                if (loan.getStatus().equals("APPROVED")) {
                    balPstmt.setDouble(1, loan.getPrincipalAmount());
                    balPstmt.setInt(2, loan.getUserId());
                    balPstmt.executeUpdate();
                    
                    // Logic: Auto-log a transaction for the audit trail
                    Transaction loanCredit = new Transaction(
                        java.time.LocalDateTime.now(),
                        "Loan Disbursement",
                        "LOAN",
                        loan.getPrincipalAmount(),
                        "Auto-approved by Credit Engine"
                    );
                    logTransaction(loan.getUserId(), loanCredit, conn);
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Loan Submission Rollback: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a user has an active or pending loan to prevent duplicate requests.
     * 
     * @param userId The ID of the user to check.
     * @return true if an 'APPROVED' or 'PENDING' loan exists.
     */
    public static boolean hasActiveLoan(int userId) {
        String query = "SELECT 1 FROM loans WHERE user_id = ? AND (status = 'APPROVED' OR status = 'PENDING')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches the current loan details for a user to display in the UI.
     * 
     * @param userId The ID of the user.
     * @return A Loan object, or null if no loan exists.
     */
    public static Loan getLatestLoan(int userId) {
        String query = "SELECT * FROM loans WHERE user_id = ? ORDER BY application_date DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Loan loan = new Loan(
                    rs.getInt("loan_id"),
                    rs.getInt("user_id"),
                    rs.getDouble("principal_amount"),
                    rs.getDouble("interest_rate"),
                    rs.getInt("term_months"),
                    rs.getDouble("monthly_income_reported"),
                    rs.getString("status"),
                    rs.getString("status_note"),
                    rs.getTimestamp("application_date").toLocalDateTime()
                );
                
                loan.setTotalPaid(rs.getDouble("total_paid")); 
                
                return loan;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
    * Retrieves all loans currently in a 'PENDING' state for background processing.
    * 
    * @return A list of Loan objects awaiting a final decision.
    */
    public static List<Loan> fetchAllPendingLoans() {
       List<Loan> pendingLoans = new ArrayList<>();
       String query = "SELECT * FROM loans WHERE status = 'PENDING'";

       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery()) {

           while (rs.next()) {
               pendingLoans.add(new Loan(
                   rs.getInt("loan_id"),
                   rs.getInt("user_id"),
                   rs.getDouble("principal_amount"),
                   rs.getDouble("interest_rate"),
                   rs.getInt("term_months"),
                   rs.getDouble("monthly_income_reported"),
                   rs.getString("status"),
                   rs.getString("status_note"),
                   rs.getTimestamp("application_date").toLocalDateTime()
               ));
           }
       } catch (SQLException e) {
           System.err.println("Error fetching pending loans: " + e.getMessage());
       }
       return pendingLoans;
    }
    
    /**
    * Finalizes a pending loan by either approving or denying it.
    * 
    * @param loanId The ID of the loan record.
    * @param userId The ID of the owner.
    * @param approve Boolean to determine the final status.
    */
    public static void finalizePendingLoan(int loanId, int userId, double amount, boolean approve) {
       String status = approve ? "APPROVED" : "DENIED";
       String note = approve ? "Post-review: Credit criteria met." : "Post-review: High debt-to-income ratio.";

       String updateLoan = "UPDATE loans SET status = ?, status_note = ? WHERE loan_id = ?";
       String updateBalance = "UPDATE users SET balance = balance + ? WHERE id = ?";

       try (Connection conn = DatabaseConnection.getConnection()) {
           conn.setAutoCommit(false);

           try (PreparedStatement lPstmt = conn.prepareStatement(updateLoan);
                PreparedStatement bPstmt = conn.prepareStatement(updateBalance)) {

               // 1. Update Loan Status
               lPstmt.setString(1, status);
               lPstmt.setString(2, note);
               lPstmt.setInt(3, loanId);
               lPstmt.executeUpdate();

               // 2. If Approved, credit the account and log the audit trail
               if (approve) {
                   bPstmt.setDouble(1, amount);
                   bPstmt.setInt(2, userId);
                   bPstmt.executeUpdate();

                   Transaction t = new Transaction(
                       java.time.LocalDateTime.now(),
                       "Loan Disbursement",
                       "LOAN",
                       amount,
                       "Loan Approved"
                   );
                   logTransaction(userId, t, conn);
               }

               conn.commit();
               System.out.println("[DB] Loan " + loanId + " finalized as " + status);

           } catch (SQLException e) {
               conn.rollback();
               e.printStackTrace();
           }
       } catch (SQLException e) {
           e.printStackTrace();
       }
    }

    /**
     * Updates a loan record in the database whenever a user makes a payment.
     * * This method connects to the database, looks up the specific loan by its ID number, 
     * and adds the new payment amount to the running total of what has been paid back so far.
     * 
     * * @param loanId The unique ID number of the loan being paid off.
     * @param paymentAmount The amount of money the user is paying toward the loan in this transaction.
     */
    public static void updateLoanBalance(int loanId, double paymentAmount) {
        String sql = "UPDATE loans SET total_paid = total_paid + ? WHERE loan_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setDouble(1, paymentAmount);
             pstmt.setInt(2, loanId);
             pstmt.executeUpdate();

        } catch (SQLException e) {
             e.printStackTrace();
        }
    }
    
    /**
    * Retrieves the current balance for a specific account type belonging to a user.
    * 
    * @param userId The ID of the logged-in user.
    * @param accountType The type of account (e.g., "Checking", "Savings").
    * @return The account balance as a double, or 0.0 if not found.
    */
    public static double getAccountBalance(int userId, String accountType) {
        String sql = "SELECT balance FROM users WHERE user_id = ?";
        double balance = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    balance = rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Could not fetch account balance: " + e.getMessage());
            e.printStackTrace();
        }

        return balance;
    }
   
    /**
     * Updates the user's balance in the 'users' table using relative math.
     * 
     * @param userId The unique ID of the user.
     * @param amountChange The amount to add (positive) or deduct (negative).
     */
    public static void updateAccountBalance(int userId, double amountChange) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amountChange);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update balance in users table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
    * Updates the status of a specific loan.
    * 
    * @param loanId The ID of the loan to update.
    * @param newStatus The new status string (e.g., "PAID", "DENIED", "APPROVED").
    */
    public static void updateLoanStatus(int loanId, String newStatus) {
        String sql = "UPDATE loans SET status = ? WHERE loan_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, loanId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update loan status: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
