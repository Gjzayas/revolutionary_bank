
package OnlineBanking;

import org.junit.Test;
import static org.junit.Assert.*;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Integration test suite for the P2P (Peer-to-Peer) Fund Transfer feature.
 * This class validates the "Atomicity" of the transfer process, ensuring that 
 * balances are updated correctly for both parties and that the corresponding 
 * transaction audit logs are generated and persisted.
 * 
 * @author Gabriel J. Zayas
 * Date: 4/08/2026
 * @version 3.0
 */
public class TransferIntegrationTest {
    
    /**
     * Tests the complete end-to-end lifecycle of a fund transfer between two accounts.
     * 
     * 
     * TEST STAGES:
     * 1. SETUP: Dynamically generates two unique dummy accounts with different 
     * starting balances to simulate a real sender-recipient scenario.
     * 2. EXECUTE: Performs the database-level transfer logic via 'UserStore.executeTransfer'.
     * 3. LOGGING: Simulates the controller behavior by generating 'Transaction' 
     * objects for both DEBIT and CREDIT entries.
     * 4. VERIFY: Asserts that the final balances in the database match the expected 
     * mathematical outcome.
     * 5. AUDIT VERIFY: Uses a Stream API check to confirm the 'senderHistory' 
     * contains the specific transfer record.
     * 6. CLEANUP: Utilizes a 'finally' block to ensure dummy data is purged regardless 
     * of test success or failure, maintaining a clean testing environment.
     * 
     * @throws SQLException If the integration with the MySQL database is interrupted.
     */
    @Test
    public void testFullTransferLifecycle() throws SQLException {
        // 1. SETUP: Create two unique dummy accounts
        // Use UUID and Math.random to prevent collisions with existing records.
        String senderUser = "sender_" + UUID.randomUUID().toString().substring(0, 5);
        String recipUser = "recip_" + UUID.randomUUID().toString().substring(0, 5);
        
        String senderAccNum = "TEST-S-" + (int)(Math.random() * 9000);
        String recipAccNum = "TEST-R-" + (int)(Math.random() * 9000);

        // Add users with initial balances
        int senderId = UserStore.addUserToDatabase(senderUser, "Sender Test", "Pass123!", senderAccNum, 1000.00, "Q", "A");
        int recipId = UserStore.addUserToDatabase(recipUser, "Recip Test", "Pass123!", recipAccNum, 200.00, "Q", "A");

        try {
            // DECISION: Verify successful creation before proceeding
            assertNotEquals("Sender creation failed", -1, senderId);
            assertNotEquals("Recipient creation failed", -1, recipId);

            // 2. EXECUTE: Transfer $300.00 from Sender to Recipient
            double transferAmount = 300.00;
            double newSenderBal = 1000.00 - transferAmount;
            double newRecipBal = 200.00 + transferAmount;

            // Atomic database update for balances
            boolean success = UserStore.executeTransfer(senderAccNum, newSenderBal, recipAccNum, newRecipBal);
            assertTrue("Database transfer execution failed", success);

            // 3. LOGGING: Manually trigger the transaction logs 
            Transaction senderEntry = new Transaction("Transfer to " + recipAccNum, "DEBIT", transferAmount, "Test Transfer");
            Transaction recipEntry = new Transaction("Transfer from " + senderAccNum, "CREDIT", transferAmount, "Test Transfer");
            
            UserStore.logTransaction(senderId, senderEntry);
            UserStore.logTransaction(recipId, recipEntry);

            // 4. VERIFY: Check Balances in Database
            // Re-fetching from DB ensures we are verifying the ACTUAL saved state.
            BankAccount updatedSender = UserStore.findAccountById(senderId);
            BankAccount updatedRecip = UserStore.findAccountById(recipId);

            assertEquals("Sender balance incorrect after transfer", 700.00, updatedSender.getBalance(), 0.01);
            assertEquals("Recipient balance incorrect after transfer", 500.00, updatedRecip.getBalance(), 0.01);

            // 5. VERIFY: Check Transaction History
            // LOOP-EQUIVALENT: Uses .anyMatch() to iterate through history and confirm logging logic.
            List<Transaction> senderHistory = UserStore.loadTransactionHistory(senderId);
            boolean foundSenderLog = senderHistory.stream().anyMatch(t -> t.getDescription().contains(recipAccNum));
            assertTrue("Sender transaction history missing the transfer record", foundSenderLog);

        } finally {
            // 6. CLEANUP: Vital for integration tests to prevent database bloat
            // Delete both users (Transaction history deletes automatically via CASCADE)
            UserStore.deleteUser(senderId);
            UserStore.deleteUser(recipId);
            
            assertNull("Cleanup failed: Sender still exists", UserStore.findAccountById(senderId));
            assertNull("Cleanup failed: Recipient still exists", UserStore.findAccountById(recipId));
            System.out.println("Integration Test Complete: Accounts created, transferred, and purged.");
        }
    }
    
}
