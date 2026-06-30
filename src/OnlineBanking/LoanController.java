
package OnlineBanking;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller class that manages the user interface for the Loan Request Center.
 * This class handles form submissions, runs automated credit approvals, manages monthly 
 * repayment workflows, and triggers dynamic visual layouts depending on a loan's status.
 * 
 * * @author Gabriel J. Zayas
 * @version 4.0
 * Date: June 19, 2026
 */
public class LoanController {

    // --- FXML UI Components ---
    /** The text entry box where the user types in the amount of money they want to borrow. */
    @FXML private TextField amountField;
    
    /** The drop-down selection menu where the user picks how many months they need to pay back the loan. */
    @FXML private ComboBox<Integer> termComboBox;
    
    /** The text entry box where the user types in their regular monthly income. */
    @FXML private TextField incomeField;
    
    /** The text label on the screen that visually displays the current state of the loan (e.g., APPROVED, PENDING, DENIED). */
    @FXML private Label statusLabel;
    
    /** The text label that shows extra explanation context or feedback from the system to the user. */
    @FXML private Label noteLabel;
    
    /** The button the user clicks to submit their completed loan application form. */
    @FXML private Button submitButton;
    
    
    // --- Containers to toggle visibility ---
    /** The layout box panel holding the loan application inputs; hidden once a loan is active. */
    @FXML private VBox formContainer;
    
    /** The layout box panel that displays the loan's status card and review details on the screen. */
    @FXML private VBox statusContainer;
    
    /** The large on-screen text display showing the balance that the user still owes on an active loan. */
    @FXML private Label loanBalanceLabel;
    
    /** The text entry box where the user types in the specific amount of money they want to pay toward their debt. */
    @FXML private TextField paymentField;
    
    /** The layout box panel holding the repayment input form and payoff action buttons. */
    @FXML private VBox paymentPanel;
    
    /** The text label that automatically calculates and prints the next required payment amount and its upcoming calendar due date. */
    @FXML private Label loanDetailLabel;

    // --- BACKGROUND PROPERTIES (System Data Trackers) ---
    /** The active user's account profile object, used to verify ownership and read checkout balance limits. */
    private BankAccount userAccount;
    
    /** A direct reference link to the main dashboard interface window to sync account details across different tabs. */
    private DashboardController parentController;
    
    /** A background repeating ticker clock that checks the database every few seconds to see if a pending loan has changed status. */
    private Timeline refreshTimeline;

    //-------------------------------------------------------------------------
    // SYSTEM INITIALIZATION
    //-------------------------------------------------------------------------

    /**
     * Initializes the view. Sets up the term options (e.g., 12, 24, 36 months)
     * and checks the database for existing loan activity.
     */
    @FXML
    public void initialize() {
        // Populate loan term options
        termComboBox.getItems().addAll(12, 24, 36, 48, 60);
        termComboBox.getSelectionModel().selectFirst();
        
        // Set Loan balance text
        loanBalanceLabel.setText("$0.00");
        
        // Ensure only the form is visible initially
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        paymentPanel.setVisible(false);
        paymentPanel.setManaged(false);
        statusContainer.setVisible(false);
        statusContainer.setManaged(false);
        
        paymentField.setOnKeyTyped(e -> paymentField.setStyle(""));
        
        // Setup Focus Listeners for formatting
        setupCurrencyField(amountField);
        setupCurrencyField(incomeField);
        setupCurrencyField(paymentField);
        
        // VALIDATION LOGIC:
        // Boolean binding that is true if any field is empty
        submitButton.disableProperty().bind(
            amountField.textProperty().isEmpty()
            .or(incomeField.textProperty().isEmpty())
            .or(termComboBox.valueProperty().isNull())
        );

        // Listener to clear error styles when the user starts typing
        amountField.setOnKeyTyped(e -> amountField.setStyle(""));
        incomeField.setOnKeyTyped(e -> incomeField.setStyle(""));
        
        // Begin the timer when a "PENDING" loan is submitted
        setupAutoRefresh();
    }
    
    /**
     * Hooks up a listener to a text box that automatically changes raw number entries 
     * into formatted currency strings the absolute moment a user clicks out of the box.
     * * @param field The text input field where currency formatting will be applied.
     */
    private void setupCurrencyField(TextField field) {
        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Focus lost
                String formatted = formatToCurrency(field.getText());
                field.setText(formatted);
            }
        });
    }
    
    //-------------------------------------------------------------------------
    // CONTROLLER LINKS & DATA INJECTION
    //-------------------------------------------------------------------------

    /**
     * Injected from the DashboardController during navigation.
     * Triggers the UI check to see if the user should see the form or their status.
     * * @param account The authenticated user's profile and active balance details.
     */
    public void setUserAccount(BankAccount account) {
        this.userAccount = account;
        
        // Check if the absolute latest loan is Denied before painting the UI on tab entry
        Loan latest = UserStore.getLatestLoan(account.getId());
        
        if (latest != null && latest.getStatus().equals("DENIED")) {
            // If the user is entering the tab fresh and the last loan was denied, 
            // show the form container directly instead of the old rejection note!
            formContainer.setVisible(true);
            formContainer.setManaged(true);
            statusContainer.setVisible(false);
            statusContainer.setManaged(false);
        
        } else {
            refreshLoanUI();
        }
    }
    
    /**
     * Links the loan sub-tab back to the master dashboard navigation system.
     * This connection allows the loan tab to tell the main screen to update global balances.
     * * @param controller The central parent Dashboard UI manager.
     */
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
    }
    
    //-------------------------------------------------------------------------
    // CORE LAYOUT MANIPULATION LOGIC
    //-------------------------------------------------------------------------

    /**
     * Determines whether to show the application form or the current loan status.
     * - Logic: Approved loans stay visible forever. 
     * - Logic: Pending loans stay visible for the background processor.
     * - Logic: Denied loans are visible during the session but revert to a form on re-login.
     */
    private void refreshLoanUI() {
        // Fetch fresh status directly from DB to handle external approvals
        Loan latestLoan = UserStore.getLatestLoan(userAccount.getId());

        boolean hasActiveLoan = latestLoan != null && 
                               (latestLoan.getStatus().equals("APPROVED") || 
                                latestLoan.getStatus().equals("PENDING") && latestLoan.getBalance() >= 0.01 ||
                                latestLoan.getStatus().equals("DENIED"));

        // Toggle main containers
        formContainer.setVisible(!hasActiveLoan);
        formContainer.setManaged(!hasActiveLoan);
        
        statusContainer.setVisible(hasActiveLoan);
        statusContainer.setManaged(hasActiveLoan);

        if (hasActiveLoan) {
            String status = latestLoan.getStatus();
        
            // Clear previous CSS styles
            statusLabel.getStyleClass().removeAll("status-approved", "status-pending", "status-denied");
            statusLabel.setText(status);
            noteLabel.setText(latestLoan.getStatusNote());

            // Handle State-Specific Logic (APPROVED vs PENDING)
            if (status.equals("APPROVED")) {
                statusLabel.getStyleClass().add("status-approved");

                // Stop the polling timer since we are no longer waiting for a status change
                if (refreshTimeline != null) {
                    refreshTimeline.stop();
                }

                // Show and update the payment panel
                paymentPanel.setVisible(true);
                paymentPanel.setManaged(true);
                loanBalanceLabel.setText(String.format("$%,.2f", latestLoan.getBalance()));
                
                // Update Monthly payment details
                updatePaymentDetails(latestLoan);

            } else if (status.equals("PENDING")) {
                statusLabel.getStyleClass().add("status-pending");

                // Hide payment panel—cannot pay a loan that isn't finalized
                paymentPanel.setVisible(false);
                paymentPanel.setManaged(false);

                // Ensure the auto-refresh timer is running to check for approval
                if (refreshTimeline != null && refreshTimeline.getStatus() != Animation.Status.RUNNING) {
                    refreshTimeline.play();
                }
            
            } else if (status.equals("DENIED")) {
                statusLabel.getStyleClass().add("status-denied");
                paymentPanel.setVisible(false);
                paymentPanel.setManaged(false);
                
                if (refreshTimeline != null) {
                    refreshTimeline.stop();
                }
            }
        
        } else {
            // If no active loan, show the form and hide status/payments
            formContainer.setVisible(true);
            formContainer.setManaged(true);

            statusContainer.setVisible(false);
            statusContainer.setManaged(false);

            paymentPanel.setVisible(false);
            paymentPanel.setManaged(false);
            
            // If no active loan, ensure timer is stopped
            if (refreshTimeline != null) {
                refreshTimeline.stop();
            }
        }
    }

    //-------------------------------------------------------------------------
    // USER ACTION HANDLERS (EVENT DRIVEN HANDLERS)
    //-------------------------------------------------------------------------

    /**
     * Captures, processes, cleans up, and submits a user's loan application form.
     * * This removes all formatting symbols, checks validation limits, and routes 
     * large loans over $50,000 or high-income requests straight to a manual review layout. 
     * Standard loans are pushed through to the instant credit decision algorithm.
     * * @param event The mouse click button event coming from the FXML file context.
     */
    @FXML
    private void handleSubmitLoan(ActionEvent event) {
        // Reset styles at the start of submission
        amountField.setStyle("");
        incomeField.setStyle("");

        try {
            // Check for empty strings just in case (as a secondary guard)
            if (amountField.getText().trim().isEmpty() || incomeField.getText().trim().isEmpty()) {
                showError("All fields are required.");
                return;
            }

            // Strip out currency formatting before parsing
            double amount = Double.parseDouble(amountField.getText().replaceAll("[$,]", ""));
            double income = Double.parseDouble(incomeField.getText().replaceAll("[$,]", ""));
            int terms = termComboBox.getValue();
            
            // Tiered Logic Thresholds
            // Logic: Amounts >= 50k or Income >= 12k/mo trigger manual review.
            final double HIGH_VALUE_THRESHOLD = 50000.00;
            final double HIGH_INCOME_THRESHOLD = 12000.00;
            
            // Create the Loan Model
            Loan newRequest = new Loan(userAccount.getId(), amount, terms, income);

            // PATH A: Background Review (High Risk/Value)
            if (amount >= HIGH_VALUE_THRESHOLD || income >= HIGH_INCOME_THRESHOLD) {
                newRequest.setStatus("PENDING");
                newRequest.setStatusNote("Manual review required due to high loan amount and/or income level.");
                
                newRequest.setPrincipleAmount(amount); 
                newRequest.setInterestRate(0.05);

                if (UserStore.submitLoanRequest(newRequest)) {
                    showInformationAlert("Application Pending", 
                        "Your request is being manually reviewed by our credit team.");
                }
            } 
            // PATH B: Instant Decision (Standard Risk)
            else {
                Loan evaluated = LoanEngine.evaluateLoan(newRequest);
                if (UserStore.submitLoanRequest(evaluated)) {
                    showInformationAlert("Instant Decision", evaluated.getStatusNote());
                }
            }

            // Final UI Updates
            if (parentController != null) parentController.refreshWelcomeMessage();
            refreshLoanUI();

        } catch (NumberFormatException e) {
            showError("Invalid amount format.");
        }
    }
    
    /**
     * Executes the repayment transactional workflow when a user pays toward their active loan balance.
     * * This performs multiple tasks in order:
     * 1. Validates input values to stop negative numbers or overpayments.
     * 2. Lowers the outstanding debt and deducts funds directly from the active checking account.
     * 3. Logs a descriptive transaction "Debit" receipt for historical record checking.
     * 4. Triggers a celebration notification alert if the balance drops to zero ($0.00), resetting the screen.
     * 5. Signals the global layout system to refresh standard dashboards.
     */
    @FXML
    private void handleMakePayment() {
        try {
            String cleanInput = paymentField.getText().replaceAll("[$,]", "");
            double paymentAmount = Double.parseDouble(cleanInput);
            
            Loan latestLoan = UserStore.getLatestLoan(userAccount.getId());

            // 1. Validation: Check for non-positive numbers or overpayments
            if (paymentAmount <= 0 || paymentAmount > latestLoan.getBalance()) {
                showError("Invalid payment amount.");
                return;
            }

            // 2. Update Database: Subtract the payment from the loan balance
            UserStore.updateLoanBalance(latestLoan.getLoanId(), paymentAmount);
            
            // Update Bank Account Balance in DB
            UserStore.updateAccountBalance(userAccount.getId(), -paymentAmount);
            
            // 3. Log the Transaction with the class constructor
            // Constructor: Transaction(description, type, amount, note)
            Transaction paymentRecord = new Transaction(
                "Loan Payment: ID #" + latestLoan.getLoanId(),  // description
                "Debit",                                        // type
                paymentAmount,                                 // amount (negative for deduction)
                "Repayment via Loan Center"                     // note
            );
            
            // Add Transaction to history
            UserStore.logTransaction(userAccount.getId(), paymentRecord);
            
            // Update local object memory for immediate use
            userAccount.setBalance(userAccount.getBalance() - paymentAmount);
            

            // 4. UI Maintenance: Clear input and refresh local loan view
            paymentField.clear();
            
            // Re-fetch the loan from DB to get the new 'total_paid' calculated balance
            Loan updatedLoan = UserStore.getLatestLoan(userAccount.getId());
            
            if (updatedLoan != null) {
                // Check if the loan is now paid off
                if (updatedLoan.getBalance() <= 0.01) {
                    // Officially mark as PAID in the database
                    UserStore.updateLoanStatus(updatedLoan.getLoanId(), "PAID");
                    
                    // Wipe out the text fields so the new form is completely clean
                    clearFormFields();

                    // Refresh the entire UI logic—this will now hide the payment panel 
                    // and show the application form because updatedLoan.getBalance() <= 0.01
                    refreshLoanUI();

                    // Show a celebration/success alert
                    showInformationAlert("Loan Paid!", "Congratulations! Your loan has been paid in full.");
                } else {
                    // Just update the label if there is still a balance
                    loanBalanceLabel.setText(String.format("$%,.2f", updatedLoan.getBalance()));
                }
            }

            // 5. Global Sync: Tell the Dashboard to update the main balance and history lists
            if (parentController != null) {
                parentController.refreshBalances();
            }

        } catch (NumberFormatException e) {
            // Handle non-numeric input (e.g., show an error dialog)
            System.err.println("Invalid payment input: " + paymentField.getText());
        }
    }

    //-------------------------------------------------------------------------
    // UTILITIES & POPUPS
    //-------------------------------------------------------------------------

    /**
     * Displays a system standard graphical modal error box to warn users of an invalid layout entry.
     * * @param message The main text statement explaining the error message.
     */
    private void showError(String message) {
        // Reuse the noteLabel or an Alert for errors
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Formats raw numeric text entries into clean financial records.
     * * @param input The unrefined string data collected directly from text controls.
     * @return A beautiful formatted string with currency signs and proper padding (e.g., $1,250.00).
     */
    private String formatToCurrency(String input) {
        if (input == null || input.trim().isEmpty()) 
            return "";
        try {
            // Remove existing symbols/commas to get a clean number
            double value = Double.parseDouble(input.replaceAll("[$,]", ""));
            return String.format("$%,.2f", value); // Formats as monetary value
        } catch (NumberFormatException e) {
            return input; // Return original if not a number
        }
    }
    
    /**
    * Displays a standard information dialog. 
    * Replaces the older showSuccessAlert for better flexibility.
    * 
    * - Logic: Used for both instant approvals and background submission notifications.
    * 
    * @param title The text for the alert header.
    * @param message The content to display in the alert body.
    */
    private void showInformationAlert(String title, String message) {
       Alert alert = new Alert(Alert.AlertType.INFORMATION);
       alert.setTitle("Loan Center");
       alert.setHeaderText(title);
       alert.setContentText(message);

       // Add the bank logo to the alert window icon if available
       Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
       var iconStream = getClass().getResourceAsStream("images/Rev_Logo_resize2.png");
       
       if (iconStream != null) {
           stage.getIcons().add(new javafx.scene.image.Image(iconStream));
       }

       alert.showAndWait();
    }
   
    /**
     * Sets up a background timer to refresh the UI.
     * - Logic: Every 10 seconds, it checks if a pending loan was updated by the processor.
     */
    private void setupAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            // Only refresh if the user is currently looking at a PENDING status
            Loan latest = UserStore.getLatestLoan(userAccount.getId());

            if (latest != null && !latest.getStatus().equals("PENDING")) {
                // Stop polling immediately now that we have a final decision
                refreshTimeline.stop();

                // Use Platform.runLater to force the JavaFX Application Thread to paint the UI changes instantly
                javafx.application.Platform.runLater(() -> {
                    // 1. Force the entire layout to recalculate containers and visibility states
                    refreshLoanUI();

                    // 2. Explicitly apply the CSS classes and text labels right here on the spot
                    if (latest.getStatus().equals("APPROVED")) {
                        statusLabel.setText(latest.getStatus());
                        statusLabel.getStyleClass().removeAll("status-pending");
                        statusLabel.getStyleClass().add("status-approved");
                        noteLabel.setText(latest.getStatusNote());

                        // Sync the global dashboard layout if necessary
                        if (parentController != null) {
                            parentController.refreshWelcomeMessage();
                            parentController.refreshBalances(); // Disburses the loan cash into the main balance label on screen!
                        }
                    } else if (latest.getStatus().equals("DENIED")) {
                        statusLabel.setText("Status: " + latest.getStatus());
                        statusLabel.getStyleClass().removeAll("status-pending");
                        statusLabel.getStyleClass().add("status-denied"); 
                        noteLabel.setText(latest.getStatusNote());
                    }

                    // 3. Force the parent layouts to request a layout pass to eliminate text truncation or rendering ghosting
                    if (formContainer.getParent() != null) {
                        formContainer.getParent().requestLayout();
                    }
                });
            }
        }));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
    }
   
    /**
     * Completely clears out and strips entries inside form inputs.
     * Use this when a loan is fully resolved or closed to return the interface to a baseline state.
     */
    private void clearFormFields() {
        // Replace these with your actual FXML IDs for the application fields
        amountField.clear();
        incomeField.clear();
        termComboBox.getSelectionModel().clearSelection();
    }
    
    /**
     * Calculates monthly payments using standard interest amortization math modeling.
     * Captures system variables and formats future due details directly onto the UI label layout.
     * * @param loan The current approved loan object model.
     */
    private void updatePaymentDetails(Loan loan) {
        if (loan == null) return;

        // 1. Mirror the LoanEngine constants and variables
        double annualRate = 0.05; // 5% flat rate as defined in LoanEngine
        double monthlyRate = annualRate / 12;
        double p = loan.getPrincipalAmount();
        int n = loan.getTermMonths();

        // 2. Amortization Formula: M = P [ i(1 + i)^n ] / [ (1 + i)^n – 1 ]
        double monthlyPayment = p * (monthlyRate * Math.pow(1 + monthlyRate, n)) 
                                / (Math.pow(1 + monthlyRate, n) - 1);

        // 3. Calculate the Due Date (1 month from today)
        LocalDate dueDate = LocalDate.now().plusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // 4. Update the Label using comma-separated formatting
        loanDetailLabel.setText(String.format("Next Payment: $%,.2f\nDue %s", 
                                monthlyPayment, 
                                dueDate.format(formatter)));
    }
}
