
package OnlineBanking;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user's bank account within the Revolutionary Bank system.
 * This model handles core financial logic, including deposits, withdrawals, and 
 * peer-to-peer transfers. It implements Serializable to allow persistent storage 
 * of account data and transaction history.
 *
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 3.0
 * 
 */
public class BankAccount {
    // Database ID
    private int id; 
    
    /** The unique identification number for this account. */
    private String accountNumber;
    
    /** The legal first and last name of the account holder. */
    private String fullName;
    
    /** The current available liquid balance in the account. */
    private double balance;
    
    /** A chronological list of all financial activities associated with this account. */
    private List<Transaction> transactionHistory;
    
    /** The security prompt used for identity verification during password recovery. */
    private String securityQuestion;
    
    /** The encrypted or plain-text answer required to pass security verification. */
    private String securityAnswer;
    
    /**
     * Constructs a new BankAccount with an initial deposit and established security credentials.
     * 
     * @param id The database id for the user
     * @param accountNumber The unique string identifying the account.
     * @param fullName The account holder's full name.
     * @param initialBalance The starting amount to be deposited upon creation.
     * @param question The chosen security question for the account.
     * @param answer The answer to the chosen security question.
     */
    public BankAccount(int id, String accountNumber, String fullName, double initialBalance, String question, String answer) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.fullName = fullName;
        this.balance = initialBalance;
        this.securityQuestion = question;
        this.securityAnswer = answer;
        this.transactionHistory = new ArrayList<>();
        
    }

    /**
     * Adds funds to the account balance and logs a "Personal Deposit" transaction.
     * 
     * @param amount The total value to be added to the balance. Must be positive.
     */
    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
            
            // 1. Update the balance in MySQL
            UserStore.updateBalance(this.accountNumber, this.balance);
            
            // 2. Add to database and update local UI list
            Transaction t = new Transaction("Deposit to account", "Deposit", amount, "Personal Deposit");
            UserStore.logTransaction(this.id, t);
            this.transactionHistory.add(0, t); // Adds to top for UI
        }
    }

    /**
     * Deducts funds from the account balance if sufficient coverage exists.
     * 
     * @param amount The value to be removed from the account.
     * @return true if the withdrawal was successful; false if funds were insufficient.
     */
    public boolean withdraw(double amount) {
        if (amount > 0 && balance >= amount) {
            this.balance -= amount;
            
            // 1. Update the balance in MySQL
            UserStore.updateBalance(this.accountNumber, this.balance);
            
            // 2. Add to database and update local UI list
            Transaction t = new Transaction("Withdrawal", "Debit", amount, "Cash Withdrawal");
            UserStore.logTransaction(this.id, t);
            this.transactionHistory.add(0, t); // Adds to top for UI
            
            return true;
        }
        return false;
    }

    /**
     * Executes a peer-to-peer money transfer using double-entry accounting.
     * This method deducts from the sender, credits the recipient, and logs the 
     * specified note for both parties.
     * 
     * @param recipientAccountNumber The account number of the person receiving funds.
     * @param amount The total value to transfer.
     * @param note A custom label or category (e.g., "Rent") for the transaction.
     * @return true if the transfer was completed; false if the recipient was not found or balance was low.
     */
    public boolean transfer(String recipientAccountNumber, double amount, String note) {
        // 1. Basic checks
        if (amount <= 0 || this.balance < amount) return false;
        if (recipientAccountNumber.equals(this.accountNumber)) return false;

        // 2. SEARCH for the recipient using your new DB-connected findAccountByNumber
        BankAccount recipient = UserStore.findAccountByNumber(recipientAccountNumber);

        if (recipient != null) {
            // 3. Calculate new balances locally
            double senderNewBalance = this.balance - amount;
            double recipientNewBalance = recipient.getBalance() + amount;

            // 4. Update the Database using a helper method (in UserStore)
            // We use a single connection to ensure both updates succeed
            boolean success = UserStore.executeTransfer(this.accountNumber, senderNewBalance, 
                                                        recipientAccountNumber, recipientNewBalance);

            if (success) {
                // Update local memory only after DB success
                this.balance = senderNewBalance;
                recipient.setBalance(recipientNewBalance);

                // 5. Log history
                String finalNote = (note == null || note.isEmpty()) ? "Standard Transaction" : note;
                this.addTransaction("Transfer to " + recipientAccountNumber, "Debit", amount, finalNote);
                recipient.addTransaction("Transfer from " + this.accountNumber, "Credit", amount, finalNote);

                return true;
            }
        }
        return false;
    }
    
    /**
     * Internal helper to append a new transaction to the account's history.
     * 
     * @param desc Description of the activity.
     * @param type The transaction type (e.g., Debit, Credit, Deposit).
     * @param amount The monetary value of the activity.
     * @param note A supplemental note or category.
     */
    private void addTransaction(String desc, String type, double amount, String note) {
        Transaction t = new Transaction(desc, type, amount, note);
        UserStore.logTransaction(this.id, t);
        this.transactionHistory.add(0, t);
    }

    // Getters
    /** @return The current available balance. */
    public double getBalance() { return balance; }
    
    /** @return The unique account number. */
    public String getAccountNumber() { return accountNumber; }
    
    /** @return The list of all past transactions. */
    public List<Transaction> getTransactionHistory() { return transactionHistory; }
    
    /** @return The full name of the account owner. */
    public String getFullName() { return fullName; }
    
    /** @return The registered security question. */
    public String getSecurityQuestion() { return securityQuestion; }
    
    /** @return The stored answer to the security question. */
    public String getSecurityAnswer() { return securityAnswer; }
    
    public int getId() { return id; }
    
    /**
    * Updates the current balance.
    * This is used during transfers to adjust the recipient's balance
    * without triggering an automatic "Deposit" transaction log.
    * 
    * @param balance The new balance value to set.
    */
   public void setBalance(double balance) {
       this.balance = balance;
   }
    
}
