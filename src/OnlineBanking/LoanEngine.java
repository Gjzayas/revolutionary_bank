
package OnlineBanking;

/**
 * Internal "Credit Engine" to simulate automated loan approval/denial.
 * * VERSION HISTORY:
 * 3.0 - Basic DTI and amount-based logic.
 * 4.0 - Promoted to major release alongside the integrated Loan Center upgrade.
 * 
 * * @author Gabriel J. Zayas
 * @version 4.0
 * Date: June 19, 2026
 */
public class LoanEngine {

    //-------------------------------------------------------------------------
    // CONSTANTS (Engine Thresholds)
    //-------------------------------------------------------------------------

    /** The maximum allowed Debt-to-Income ratio (40%). A user's loan payment cannot eat up more than this percentage of their monthly income. */
    private static final double MAX_DTI_RATIO = 0.40; // 40% threshold
    
    /** The standard flat annual interest rate (5%) applied to all approved loans. */
    private static final double ANNUAL_INTEREST_RATE = 0.05; // 5% flat rate
    
    /** The dollar amount limit ($50,000.00). Anything above this number cannot be approved automatically and skips to "human review". */
    private static final double MANUAL_REVIEW_THRESHOLD = 50000.00;

    
    //-------------------------------------------------------------------------
    // CREDIT EVALUATION LOGIC
    //-------------------------------------------------------------------------

    /**
     * Reviews a loan application and automatically decides if it should be Approved, Denied, or held for Pending review.
     * * It locks in the current interest rate, checks for a valid income, calculates the expected 
     * monthly payment, and measures it against the user's monthly income to verify safety limits.
     * * @param loan The loan application object that needs evaluation.
     * @return The updated loan object containing the final decision and explanatory notes.
     */
    public static Loan evaluateLoan(Loan loan) {
        // Set the Interest rate
        loan.setInterestRate(ANNUAL_INTEREST_RATE);

        // Validation: Prevent division by zero if input is somehow 0
        if (loan.getMonthlyIncome() <= 0) {
            loan.setStatus("DENIED");
            loan.setStatusNote("Denied: Valid income must be reported for credit assessment.");
            return loan;
        }

        double monthlyRate = ANNUAL_INTEREST_RATE / 12;
        double principal = loan.getPrincipalAmount();
        int TermMonths = loan.getTermMonths();
        
        // Calculate monthly payment using the amortization formula
        double monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, TermMonths)) / (Math.pow(1 + monthlyRate, TermMonths) - 1);
        double dtiRatio = monthlyPayment / loan.getMonthlyIncome();

        // Rule 1: Debt-to-Income Ratio
        if (dtiRatio > MAX_DTI_RATIO) {
            loan.setStatus("DENIED");
            loan.setStatusNote(String.format("Denied: Projected payment ($%.2f) exceeds debt-to-income safety limits.", monthlyPayment));
        
        // Rule 2: Principal Threshold for Manual Review
        } else if (principal > MANUAL_REVIEW_THRESHOLD) {
            loan.setStatus("PENDING");
            loan.setStatusNote("Pending: Requests over $50,000 require manual underwriter verification.");
        
        // Rule 3: Approval
        } else {
            loan.setStatus("APPROVED");
            loan.setStatusNote("Approved: Congratulations! Your loan meets our automated credit standards.");
        }

        return loan;
    }
}
