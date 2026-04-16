
package OnlineBanking;

import static org.junit.Assert.*;
import org.junit.Test;
import java.sql.*;
import java.util.UUID;

/**
 * Automated Unit Test suite for verifying account deletion and data integrity.
 * This class ensures that removing a user from the 'users' table correctly 
 * triggers a cascading delete of all dependent records in the 'transactions' table.
 * 
 * @author Gabriel J. Zayas
 * Date: 4/08/2026
 * @version 3.0
 */
public class UserDeletionTest {
    
    /**
     * Comprehensive test to verify that deleting an account wipes all traces of the user.
     * 
     * TEST LOGIC (Setup-Execute-Verify):
     * 1. SETUP: Generates a unique dummy user with a random UUID to avoid Primary Key 
     * conflicts with existing test data.
     * 2. SEED: Injects two distinct transactions to establish a baseline for deletion.
     * 3. EXECUTE: Invokes UserStore.deleteUser() which executes the SQL DELETE command.
     * 4. VERIFY: Asserts that both the parent (User) and children (Transactions) 
     * are no longer retrievable.
     * 
     * @throws SQLException If database connectivity or constraints fail during the test.
     */
    @Test
    public void testFullAccountAndHistoryDeletion() throws SQLException {
        // SETUP: Use UUID substring to create a unique username
        String dummyUser = "test_user_" + UUID.randomUUID().toString().substring(0, 8);
        int userId = createDummyUser(dummyUser);
        
        assertTrue("Failed to create dummy user for test.", userId != -1);

        // SEED DATA: Populate transaction history for the dummy user
        insertTestTransaction(userId, "Initial Deposit", "CREDIT", 1000.00);
        insertTestTransaction(userId, "Test Purchase", "DEBIT", 50.00);

        // Verify setup worked by checking row count
        assertEquals("Transactions should be present before deletion.", 2, countTransactions(userId));

        // EXECUTE: Perform the deletion
        boolean isDeleted = UserStore.deleteUser(userId);
        assertTrue("UserStore.deleteUser returned false.", isDeleted);

        // VERIFY: Database Referential Integrity (Cascading check)
        assertEquals("Transactions were not removed via CASCADE.", 0, countTransactions(userId));

        // VERIFY: Ensure parent record is purged
        assertFalse("User record still exists in the database.", checkUserExists(userId));
    }

    /**
     * Specific focus test for the ON DELETE CASCADE database constraint.
     * This test ensures that the application logic does not leave "orphaned" 
     * transactions in the database after a user is deleted.
     * 
     * * LOGIC:
     * - This test specifically targets the relational integrity between the 
     * 'users' and 'transactions' tables.
     * - By deleting the parent user record, we verify that the MySQL engine 
     * automatically purges all child transaction records.
     * 
     * @throws SQLException if a database access error occurs or the connectivity 
     * to the revolutionary_bank schema is lost.
     */
    @Test
    public void testDeleteUserRemovesTransactionHistory() throws SQLException {
        // SETUP: Use UUID substring to create a unique username
        String uniqueUsername = "test_user_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        int userId = createDummyUser(uniqueUsername);
        
        // Ensure the dummy user was created successfully
        assertTrue("Failed to create dummy user for test.", userId != -1);
        
        // SEED DATA: Add verification transactions for the new dummy user
        insertTestTransaction(userId, "Test Deposit", "CREDIT", 500.00);
        insertTestTransaction(userId, "Test Withdrawal", "DEBIT", 50.00);

        // Pre-verification: Confirm transactions are present in the database
        int initialCount = countTransactions(userId);
        assertEquals("Transactions should exist before deletion.", 2, initialCount);

        // EXECUTE: Call the deletion method from UserStore
        boolean isDeleted = UserStore.deleteUser(userId);
        assertTrue("UserStore.deleteUser should return true on successful execution.", isDeleted);

        // VERIFY: Confirm cascading delete removed all records in the transactions table
        int finalCount = countTransactions(userId);
        assertEquals("All transactions must be removed via ON DELETE CASCADE.", 0, finalCount);
        
        // FINAL VERIFY: Confirm the dummy user record is gone
        assertFalse("User record should be deleted from the users table.", checkUserExists(userId));
    }

    // --- Database Helper Methods ---
    
    /**
     * Programmatically creates a user for testing purposes.
     * 
     * LOGIC:
     * - Uses 'Statement.RETURN_GENERATED_KEYS' to retrieve the auto-incremented ID 
     * immediately after insertion for use in subsequent test steps.
     * @param username The unique test username.
     * @return The auto-generated Primary Key ID.
     */
    private int createDummyUser(String username) throws SQLException {
        String query = "INSERT INTO users (username, password, full_name, account_number, security_question, security_answer) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, "testPass123");
            pstmt.setString(3, "Test User");
            pstmt.setString(4, "ACC-" + System.currentTimeMillis()); // Unique account_number
            pstmt.setString(5, "Security Question");
            pstmt.setString(6, "Security Answer");
            
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /**
     * Seeds a transaction record for a specific user.
     * @param userId The ID of the dummy user.
     * @param desc Transaction description.
     * @param type CREDIT or DEBIT.
     * @param amt Monetary value.
     */
    private void insertTestTransaction(int userId, String desc, String type, double amt) throws SQLException {
        String query = "INSERT INTO transactions (transaction_id, user_id, description, type, amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setInt(2, userId);
            pstmt.setString(3, desc);
            pstmt.setString(4, type);
            pstmt.setDouble(5, amt);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Utility to count existing rows in the transaction table for a user.
     * @return The count of transactions found.
     */
    private int countTransactions(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM transactions WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Checks if a user record still exists in the database.
     * @return true if a row is found with the given ID.
     */
    private boolean checkUserExists(int userId) throws SQLException {
        String query = "SELECT id FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
}
