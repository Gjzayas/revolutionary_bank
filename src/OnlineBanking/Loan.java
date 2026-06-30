
package OnlineBanking;

import java.time.LocalDateTime;

/**
 * Data model representing a user's loan application and its current status.
 * This class tracks how much money was borrowed, the interest rate, the payment progress,
 * and whether the loan is pending, approved, or denied.
 * 
 * @author Gabriel J. Zayas
 * Date: 6/19/2026
 * @version 4.0
 */
public class Loan {
    
    //-------------------------------------------------------------------------
    // INSTANCE VARIABLES
    //-------------------------------------------------------------------------

    /** The unique ID number for the loan (generated automatically by the database). */
    private int loanId;
    
    /** The ID number of the bank user who applied for this loan. */
    private int userId;
    
    /** The original amount of money the user wants to borrow. */
    private double principalAmount;
    
    /** The annual percentage interest rate evaluated for the loan (e.g., 0.05 for 5.0%). */
    private double interestRate;
    
    /** How many months the user has to pay back the loan (the loan term). */
    private int termMonths;
    
    /** The declared regular monthly income of the applicant used during automated processing. */
    private double monthlyIncome;
    
    /** The total amount of money the user has paid back so far. Starts at 0.0. */
    private double totalPaid = 0.0;
    
    /** The current status of the loan. Usually "PENDING", "APPROVED", or "DENIED". */
    private String status;
    
    /** Editorial system context added during evaluation (e.g., reason code or processor flags). */
    private String statusNote;
    
    /** The exact date and time when the user submitted the application. */
    private LocalDateTime applicationDate;

    //-------------------------------------------------------------------------
    // CONSTRUCTORS
    //-------------------------------------------------------------------------

    /**
     * Constructor used when a user applies for a brand-new loan.
     * This creates a fresh application with a default status of "PENDING".
     * * @param userId          The ID of the user applying.
     * @param principalAmount The amount of money they want to borrow.
     * @param termMonths      How many months they want to pay it back.
     * @param monthlyIncome   How much money they make per month.
     */
    public Loan(int userId, double principalAmount, int termMonths, double monthlyIncome) {
        this.userId = userId;
        this.principalAmount = principalAmount;
        this.termMonths = termMonths;
        this.monthlyIncome = monthlyIncome;
        this.status = "PENDING";
    }

    /**
     * Constructor used when loading an existing loan from the database.
     * This recreates the complete loan history with all its saved details.
     * * @param loanId          The unique database ID of the loan.
     * @param userId          The ID of the borrowing user.
     * @param principalAmount The original borrowed amount.
     * @param interestRate    The approved interest rate.
     * @param termMonths      The length of the loan in months.
     * @param monthlyIncome   The user's saved monthly income.
     * @param status          The current status (Approved/Pending/Denied).
     * @param statusNote      The system log notes for this loan.
     * @param applicationDate The exact time the loan was created.
     */
    public Loan(int loanId, int userId, double principalAmount, double interestRate, 
                int termMonths, double monthlyIncome, String status, String statusNote, LocalDateTime applicationDate) {
        this.loanId = loanId;
        this.userId = userId;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.monthlyIncome = monthlyIncome;
        this.status = status;
        this.statusNote = statusNote;
        this.applicationDate = applicationDate;
    }

    //-------------------------------------------------------------------------
    // GETTERS, SETTERS, AND LOGIC METHODS
    //-------------------------------------------------------------------------

    /**
     * Gets the original amount of money borrowed.
     * * @return The principal amount as a double.
     */
    public double getPrincipalAmount() { 
        return principalAmount; }
    
    /**
     * Sets or changes the original amount of money borrowed.
     * * @param amount The new principal amount.
     * @return The updated principal amount.
     */
    public double setPrincipleAmount(double amount) {
        return this.principalAmount = amount;
    }
    
    /**
     * Gets the total number of months allowed to pay back the loan.
     * * @return The term length in months.
     */
    public int getTermMonths() { 
        return termMonths; }
    
    /**
     * Gets the monthly income the user reported on their application.
     * * @return The user's monthly income.
     */
    public double getMonthlyIncome() { 
        return monthlyIncome; }
    
    /**
     * Updates the user's monthly income value.
     * * @param monthlyIncome The new monthly income amount.
     */
    public void setMonthlyIncome(double monthlyIncome) {
         this.monthlyIncome = monthlyIncome; }
    
    /**
     * Gets the current text status of the loan (e.g., "PENDING", "APPROVED").
     * * @return The status text.
     */
    public String getStatus() { 
        return status; }
    
    /**
     * Changes the text status of the loan.
     * * @param status The new status text to apply.
     */
    public void setStatus(String status) { 
        this.status = status; }
    
    /**
     * Gets the system note or reasoning behind the loan's current status.
     * * @return The status note string.
     */
    public String getStatusNote() { 
        return statusNote; }
    
    /**
     * Changes or updates the explanation note for the loan's status.
     * * @param statusNote The new descriptive note.
     */
    public void setStatusNote(String statusNote) { 
        this.statusNote = statusNote; }
    
    /**
     * Gets the user ID of the person who owns this loan.
     * * @return The owner's user ID integer.
     */
    public int getUserId() {
        return userId;
    }
    
    /**
     * Gets the unique database ID number assigned to this loan.
     * * @return The loan ID integer.
     */
    public int getLoanId() {
        return loanId;
    }
    
    /**
     * Calculates the true outstanding payoff balance remaining on the current loan lifecycle.
     * This formula aggregates total raw calculated simple interest against the primary principal 
     * over the lifetime term, subtracting all recorded repayment transaction ledger weights.
     * * Formula used: (Principal + (Principal * Rate * (Term / 12))) - Total Paid
     * * @return The net remaining account payable balance rounded uniformly to two decimal places.
     */
    public double getBalance() {
        // If interestRate is 0.05 (5%), calculate total interest over the term
        double totalInterest = principalAmount * interestRate * (termMonths / 12.0);
        double totalToPay = principalAmount + totalInterest;
        
        // Rounding to 2 decimal places to ensure UI consistency
        return Math.round((totalToPay - totalPaid) * 100.0) / 100.0;
    }

    /**
     * Updates the running tracker for how much total money has been paid back so far.
     * * @param totalPaid The new running total of all payments made.
     */
    public void setTotalPaid(double totalPaid) {
        this.totalPaid = totalPaid;
    }
    
    /**
     * Gets the running tracker for how much total money has been paid back so far.
     * * @return The total amount paid back as a double.
     */
    public double getTotalPaid() {
        return this.totalPaid;
    }
    
    /**
    * Sets the annual interest rate for the loan.
    * This should be called by the LoanEngine during evaluation.
    * 
    * @param interestRate The annual rate (e.g., 0.05 for 5%)
    */
    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    /**
     * Gets the annual interest rate multiplier assigned to this loan.
     * * @return The interest rate as a double decimal.
     */
    public double getInterestRate() {
        return this.interestRate;
    }
   
    /**
     * Gets the exact date and time stamp of when this loan was originally submitted.
     * * @return The original application timestamp.
     */
    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    /**
     * Sets the official historical application timestamp.
     * Used primarily when reading existing records back out of database storage.
     * * @param applicationDate The original submission timestamp to register.
     */
    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }
}
