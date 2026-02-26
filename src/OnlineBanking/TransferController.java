
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
 * @version 2.0
 * 
 */
public class TransferController {

    /** Input field for the recipient's unique account identification number. */
    @FXML private TextField recipientIdField;
    
    /** Input field for the monetary value to be transferred. Supports commas and currency symbols. */
    @FXML private TextField amountField;
    
    /** Input field for an optional personalized note or category (e.g., "Rent", "Groceries"). */
    @FXML private TextField noteField;
    
    /** Label used to provide real-time feedback (Success/Error) to the user regarding the transfer. */
    @FXML private Label transferStatusLabel;

    /** The injected BankAccount model representing the sender's account. */
    private BankAccount account;
    
    /**
     * Standard JavaFX initialization method.
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
    }
    
    /**
     * Helper method to clean and re-format the amount input field.
     * Uses String.format to provide a professional banking aesthetic 
     * while the user is still on the screen.
     */
    private void formatTransferAmount() {
        String input = amountField.getText().trim();
        
        if (input.isEmpty()) return;

        try {
            // 1. Strip any existing symbols to prevent errors
            String cleanInput = input.replaceAll("[,\\$]", "");
            double value = Double.parseDouble(cleanInput);

            // 2. Format with dollar sign, thousands separator, and 2 decimal places
            String formatted = String.format("$%,.2f", value);
            
            // 3. Update the UI field
            amountField.setText(formatted);
            
        } catch (NumberFormatException e) {
            // Silence errors here so the user can still edit the field 
            // handleTransfer() will show the formal error message if the user clicks 'Transfer'
        }
    }

    /**
     * Injects the authenticated user's account into this controller.
     * This method is called by the DashboardController during navigation to ensure 
     * the transfer is debited from the correct source.
     * * @param account The BankAccount instance belonging to the current user.
     */
    public void setAccount(BankAccount account) {
        this.account = account;
    }

    /**
     * Orchestrates the transfer process by validating inputs, sanitizing currency 
     * strings, and communicating with the BankAccount model to execute the transaction.
     * * This method implements robust input handling by stripping non-numeric 
     * characters (like commas and dollar signs) to prevent parsing errors.
     * 
     * * * Updates: Added explicit verification for recipient existence to provide 
     * more accurate error feedback to the user.
     * 
     * * Updates: Added a Confirmation Alert dialog to prevent accidental transactions.
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
                    setStatus("Transfer Successful!\nNew Balance: $" + String.format("%,.2f", account.getBalance()), Color.GREEN);
                    clearFields();
                    if (noteField != null) noteField.clear();
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
     * * @param message The text to display to the user.
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
    }
}
