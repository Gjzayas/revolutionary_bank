
package OnlineBanking;

import java.time.LocalDateTime;

/**
 * Represents a single financial event within a BankAccount.
 * This class captures the "who, what, when, and how much" of every account activity.
 * Updated for MySQL compatibility by removing serialization.
 * 
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 3.0
 * 
 */
public class Transaction {
    
    /** The exact timestamp when the transaction occurred. */
    private final LocalDateTime date;
    
    /** A text description of the activity (e.g., "Transfer to 1000-6972"). */
    private final String description;
    
    /** The classification of the transaction: "Debit", "Credit", or "Deposit". */
    private final String type; // e.g., "Deposit", "Transfer", "Withdrawal"
    
    /** The numerical monetary value of the transaction. */
    private final double amount;
    
    /** A user-provided note or category for personal record-keeping (e.g., "Rent"). */
    private final String note;
    
    /**
     * Constructs a new Transaction record with a timestamp generated at the moment of creation.
     * 
     * @param description A brief explanation of the transaction.
     * @param type The transaction category (Debit/Credit/Deposit).
     * @param amount The dollar amount involved.
     * @param note A custom memo or tag provided by the user.
     */
    public Transaction(String description, String type, double amount, String note) {
        // Captures the current system time
        this.date = LocalDateTime.now();
        this.description = description;
        this.type = type;
        this.amount = amount;
        this.note = note;
    }
    
    /**
     * Constructor used when LOADING existing transaction data from the MySQL database.
     * 
     * This constructor is specifically designed for the data retrieval layer (e.g., UserStore),
     * allowing the application to preserve the original point-in-time timestamp generated 
     * by the database rather than creating a new current timestamp.
     * 
     * @param date The specific LocalDateTime retrieved from the 'transaction_date' database column.
     * @param description The descriptive label for the record (e.g., "Transfer to ACC-1234").
     * @param type The category of the transaction (e.g., DEBIT, CREDIT, TRANSFER).
     * @param amount The monetary value of the transaction.
     * @param note The optional personalized memo or security note associated with the record.
     */
    public Transaction(LocalDateTime date, String description, String type, double amount, String note) {
        this.date = date;
        this.description = description;
        this.type = type;
        this.amount = amount;
        this.note = note;
    }

    // Getters (Required for TableView to display data)
    /** @return The date and time the transaction was processed. */
    public LocalDateTime getDate() { 
        return date; }
    
    /** @return The description of the transaction. */
    public String getDescription() { 
        return description; }
    
    /** @return The type of transaction (e.g., "Debit" or "Credit"). */
    public String getType() { 
        return type; }
    
    /** @return The monetary value of the transaction. */
    public double getAmount() { 
        return amount; }
    
    /** @return The personal note or category tag associated with the transaction. */
    public String getNote() { 
        return note; }
    
}
