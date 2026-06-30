
package OnlineBanking;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A background service that simulates a human review process for high-value loans.
 * It periodically wakes up, pulls any applications marked as 'PENDING' from the 
 * database, automatically assesses them against internal safety rules, and finalizes them.
 * Formally isolated into an independent asynchronous background scheduler thread.
 * 
 * @author Gabriel J. Zayas
 * @version 4.0
 * Date: June 19, 2026
 */
public class LoanProcessor {

    //-------------------------------------------------------------------------
    // SYSTEM TRACKERS (Thread Pools)
    //-------------------------------------------------------------------------

    /** A single background worker thread dedicated entirely to running the loan review timer independently from the user interface. */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    //-------------------------------------------------------------------------
    // SERVICE CONTROL METHODS
    //-------------------------------------------------------------------------

    /**
     * Starts up the background thread worker.
     * * This kicks off the scheduler, waiting exactly 60 seconds (1 minute) to begin 
     * its first look, and then repeats the review sweep every 60 seconds continuously.
     */
    public void startService() {
        System.out.println("[System] Loan Background Processor Started...");
        scheduler.scheduleAtFixedRate(this::processPendingLoans, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Wakes up on a timer to fetch, evaluate, and finalize outstanding applications.
     * * This pulls all pending objects from storage, reads their requested amount against 
     * the applicant's reported monthly earnings, and passes the final decision 
     * over to the ACID-compliant data layer.
     */
    private void processPendingLoans() {
        // - Step 1: Fetch all loans with 'PENDING' status from UserStore
        List<Loan> pendingList = UserStore.fetchAllPendingLoans();

        if (pendingList.isEmpty()) {
            return; // Exit silently if there are no 'PENDING' loans
        }

        System.out.println("[Processor] Found " + pendingList.size() + " loans to review.");

        for (Loan loan : pendingList) {
            // - Step 2: Decision Logic 
            // - Rule: Deny if the loan amount is more than 10x the monthly income.
            // - Retrieve the income stored within the Loan object.
            double amount = loan.getPrincipalAmount();
            double income = loan.getMonthlyIncome();
            
            boolean decision = amount <= (income * 10);
            
            if (!decision) {
                System.out.println("[Processor] Denied Loan ID " + loan.getLoanId() + " (Debt-to-Income too high).");
            
            } else {
                System.out.println("[Processor] Approved Loan ID " + loan.getLoanId() + ".");
            }

            // - Step 3: Use the ACID-compliant finalize method to approve the loan
            UserStore.finalizePendingLoan(
                loan.getLoanId(), 
                loan.getUserId(), 
                loan.getPrincipalAmount(), 
                decision
            );
        }
    }

    /**
     * Gracefully shuts down the background processor thread when the application closes.
     * * This tells the process to stop accepting new tasks, gives active evaluations 
     * up to 5 seconds to finish saving to the database, and forces an immediate cutoff 
     * if it hangs past that window.
     */
    public void stopService() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
