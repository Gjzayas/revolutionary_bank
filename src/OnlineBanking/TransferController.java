
package OnlineBanking;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Controller for the Transfer sub-view within the Dashboard.
 * This class facilitates peer-to-peer fund transfers by capturing recipient 
 * details, validating monetary input, and processing the transaction through 
 * the BankAccount model.
 * 
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 3.0
 * 
 */
public class TransferController {

    /** Input field for the recipient's unique account identification number. */
    @FXML private TextField recipientIdField;
    
    /** Input field for the monetary value. Sanitized via regex to support formatted strings. */
    @FXML private TextField amountField;
    
    /** Input field for an optional personalized note or category (e.g., "Rent", "Groceries"). */
    @FXML private TextField noteField;
    
    /** Label used to provide real-time feedback (Success/Error) to the user regarding the transfer. */
    @FXML private Label transferStatusLabel;

    /** The injected BankAccount instance belonging to the current session user. */
    private BankAccount account;
    
    /**
     * Sets up a Focus Listener on the amountField to provide real-time 
     * currency formatting (e.g., converting "1200" to "$1,200.00") 
     * as soon as the user finishes typing.
     */
    @FXML
    public void initialize() {
        // Add listener for when the user clicks away (loses focus)
        amountField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // newValue is false when the user loses focus
            if (!newValue) { 
                formatTransferAmount();
            }
        });
        
        // Group fields for automated listener attachment
        TextField[] fields = {recipientIdField, amountField, noteField};

        // LOOP: Attach a focus listener to each input field to manage the status label visibility.
        for (TextField field : fields) {
            field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                
                // DECISION: Only trigger logic when the user enters (focuses) a field. 
                if (isNowFocused) {
                    String currentMsg = transferStatusLabel.getText();
                    
                    // GUARD: If there is no message currently displayed, there is nothing to clear.
                    if (currentMsg == null || currentMsg.isEmpty()) 
                        return;

                    // Identify the current type of message based on its display color.
                    Color currentColor = (Color) transferStatusLabel.getTextFill();

                    // 1. If it's an ERROR (Red), clear it immediately when the user click back
                    if (currentColor.equals(Color.RED)) {
                        transferStatusLabel.setText("");
                    } 
                    // 2. If it's SUCCESS (Green), only clear it if the user is starting a new entry
                    else if (currentColor.equals(Color.GREEN)) {
                        if (field.getText().isEmpty()) {
                            transferStatusLabel.setText("");
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Re-formats the raw numerical input into a standard currency string.
     * 
     * LOGIC:
     * - Uses '.replaceAll("[,\\$]", "")' to strip existing formatting characters.
     * - Applies 'String.format("$%,.2f", value)' to add comma separators and 
     * two decimal places.
     */
    private void formatTransferAmount() {
        String input = amountField.getText().trim();
        
        if (input.isEmpty()) return;

        try {
            // Strip any existing symbols to prevent errors
            String cleanInput = input.replaceAll("[,\\$]", "");
            double value = Double.parseDouble(cleanInput);

            // Format with dollar sign, thousands separator, and 2 decimal places
            String formatted = String.format("$%,.2f", value);
            
            // Update the UI field
            amountField.setText(formatted);
            
        } catch (NumberFormatException e) {
            // Silently fail to allow handleTransfer() to provide the formal error message.
        }
    }

    /**
     * Injects the authenticated user's account into this controller.
     * This method is called by the DashboardController during navigation to ensure 
     * the transfer is debited from the correct source.
     * 
     * @param account The BankAccount instance belonging to the current user.
     */
    public void setAccount(BankAccount account) {
        this.account = account;
    }

    /**
     * Orchestrates the transfer workflow through validation, confirmation, and execution.
     * 
     * * LOGIC & DECISIONS:
     * 1. EMPTY CHECK: Prevents processing if mandatory fields are blank.
     * 2. SANITIZATION: Removes currency symbols to avoid Double.parseDouble() crashes.
     * 3. LOGIC VALIDATION: Checks for positive amounts and prevents self-transfers.
     * 4. RECIPIENT VERIFICATION: Calls UserStore to ensure the target account is valid.
     * 5. CONFIRMATION DIALOG: Interrupts the workflow to ensure the user intended the transfer.
     * 6. BRANDING: Injects the "Rev_Logo" into the alert window for a professional feel.
     * 7. REFRESH: If successful, it re-fetches the balance from the database to 
     * ensure local consistency.
     */
    @FXML
    private void handleTransfer() {
        String recipient = recipientIdField.getText();
        String amountText = amountField.getText();
        String note = noteField.getText();

        // 1. Basic Validation: Ensure mandatory fields are not empty
        if (recipient.isEmpty() || amountText.isEmpty()) {
            setStatus("Please fill in all fields.", Color.RED);
            return;
        }

        try {
            // 2. Data Sanitization: Use Regex to remove "," and "$" characters.
            // This allows users to type "1,000.00" or "$1000" without causing a crash.
            String cleanAmount = amountText.replaceAll("[,\\$]", "");

            // 3. Parse the cleaned string into a double for calculation
            double amount = Double.parseDouble(cleanAmount);

            // 4. Logic Validation: Ensure the transfer amount is a positive value
            if (amount <= 0) {
                setStatus("Amount must be greater than zero.", Color.RED);
                return;
            }
            
            // 5. Recipient Verification
            // Check the UserStore to see if this account number actually exists
            if (UserStore.findAccountByNumber(recipient) == null) {
                setStatus("Recipient account not found.", Color.RED);
                return;
            }
            
            // 6. Self-Transfer Validation
            // Check if the recipient account number matches the current user's account number
            if (recipient.equals(this.account.getAccountNumber())) {
                setStatus("Cannot transfer to the same account.", Color.RED);
                return;
            }

            // 7. Confirmation Dialog
            // Show a professional Alert to confirm the user's intent
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Revolutionary Bank - Transfer Confirmation");
            alert.setHeaderText("Transferring Funds");
            alert.setContentText(String.format("Are you sure you want to send $%,.2f to account %s?", amount, recipient));
            
            // CUSTOM ICON LOGIC:
            // 8. Get the window (Stage) of the alert
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            
            // 9. Add bank logo to the window's icon list
            try {
                var iconStream = getClass().getResourceAsStream("images/Rev_Logo_resize2.png");
                if (iconStream != null) {
                    // Constructor: (stream, requestedWidth, requestedHeight, preserveRatio, smooth)
                    // Set requested size to 64x64 and smooth=true for high-quality scaling
                    Image brandIcon = new Image(iconStream, 64, 64, true, true);
                    alertStage.getIcons().clear();
                    alertStage.getIcons().add(brandIcon);
                }
            } catch (Exception e) {
                System.err.println("Note: Alert icon could not be loaded.");
            }

            // Show the dialog and wait for the user's response
            Optional<ButtonType> result = alert.showAndWait();
            
            // Only proceed if the user clicks OK
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                
                // 10. Execute Model Logic
                boolean success = account.transfer(recipient, amount, note);

                if (success) {
                    // REFRESH: Fetch the latest data from DB to ensure our local object 
                    // matches the updated balance in MySQL
                    BankAccount updatedAccount = UserStore.findAccountByNumber(account.getAccountNumber());
                    if (updatedAccount != null) {
                        this.account.setBalance(updatedAccount.getBalance());
                    }

                    // Update UI with the fresh balance from the database
                    setStatus("Transfer Successful!\nNew Balance: $" + String.format("%,.2f", account.getBalance()), Color.web("#00FF7F"));
                    clearFields();
                
                } else {
                    setStatus("Insufficient funds for this transfer.", Color.RED);
                }
            } else {
                // User cancelled the transfer
                setStatus("Transfer cancelled.", Color.GRAY);
            }

        } catch (NumberFormatException e) {
            // Catches cases where the user enters non-numeric text that cannot be sanitized
            setStatus("Invalid Entry. Please enter a valid monetary number.", Color.RED);
        }
    }

    /**
     * Updates the status label with a specific message and color.
     * 
     * @param message The text to display to the user.
     * @param color The Color (e.g., RED for errors, GREEN for success) to apply to the text.
     */
    private void setStatus(String message, Color color) {
        transferStatusLabel.setText(message);
        transferStatusLabel.setTextFill(color);
    }

    /**
     * Resets the input fields to an empty state following a successful transaction.
     */
    private void clearFields() {
        recipientIdField.clear();
        amountField.clear();
        noteField.clear();
    }
}
