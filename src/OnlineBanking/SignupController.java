
package OnlineBanking;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Controller for the account registration/signup interface.
 * This class manages the creation of new bank accounts by validating user input,
 * enforcing password requirements, and initializing the account with a random 
 * account number and an initial deposit in the UserStore.
 * Features dynamic input masking and automated currency formatting via FocusProperty listeners.
 * 
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 2.0
 * 
 */
public class SignupController {
    
    /** Field for the user's legal full name. */
    @FXML private TextField fullNameField;
    
    /** Field for the desired username; forced to lowercase for consistency. */
    @FXML private TextField usernameField;
    
    /** Field for the user to define their own recovery question. */
    @FXML private TextField questionField;
    
    /** Field for the answer to the recovery question. */
    @FXML private TextField answerField;
    
    /** Secure field for the primary account password. */
    @FXML private PasswordField passwordField;
    
    /** Secure field used to verify that the password matches the primary input. */
    @FXML private PasswordField confirmPasswordField;
    
    /** Field for the starting balance of the new account. */
    @FXML private TextField initialDepositField;
    
    /** Label used to provide real-time validation and success feedback. */
    @FXML private Label messageLabel;
    
    /**
     * Initializes the view by clearing focus from input fields using Platform.runLater.
     * This provides a cleaner initial UI and prevents immediate validation triggers.
     * Sets up the initial focus and adds a FocusListener 
     * to the initialDepositField to provide automatic currency formatting.
     */
    @FXML
    public void initialize() {
        
        // Automatic Currency Formatting Logic
        // This listens for when the user clicks IN or OUT of the deposit field.
        initialDepositField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // newValue is false when the user "blurs" (clicks away) from the field
            if (!newValue) { 
                formatDepositInput();
            }
        });
    }

    /**
     * Orchestrates the signup process by validating all fields and creating a new 
     * BankAccount object.
     * * Technical Note: This method implements robust currency handling. It uses 
     * Regex sanitization to strip commas and dollar signs from the initial deposit 
     * string, allowing for flexible user input (e.g., "$5,000.00" -> 5000.0).
     * 
     * * This method performs:
     * 1. Null/Empty field validation.
     * 2. Password matching verification.
     * 3. Username availability checks via UserStore.
     * 4. Monetary parsing for the initial deposit.
     * 5. Automated generation of a unique account number (Format: 1000-xxxx).
     * * @param event The ActionEvent triggered by the "Sign Up" button.
     */
    @FXML
    private void handleSignup(ActionEvent event) {
        try {
            String name = fullNameField.getText();
            String user = usernameField.getText().trim().toLowerCase();
            String pass = passwordField.getText();
            String confirmPass = confirmPasswordField.getText();
            String question = questionField.getText();
            String answer = answerField.getText();
            String depositStr = initialDepositField.getText();

            // 1. Mandatory Field Check
            if (name.isEmpty() || user.isEmpty() || pass.isEmpty() || 
                    confirmPass.isEmpty() || depositStr.isEmpty() || 
                    question.isEmpty() || answer.isEmpty()) {
                messageLabel.setText("Please fill in all fields.");
                return;
            }
            
            // 2. Security Check: Password Matching
            if (!pass.equals(confirmPass)) {
                messageLabel.setText("Passwords do not match.");
                return;
            }
            
            // 3. DATA SANITIZATION (The Update)
            // This replaces any comma (,) or dollar sign ($) with an empty string.
            // Example: "1,250.50" becomes "1250.50"
            String cleanDeposit = depositStr.replaceAll("[,\\$]", "");
            
            // 4. Financial Validation: Parse the cleaned string
            double deposit = Double.parseDouble(cleanDeposit);
            
            // 5. Uniqueness Check: Prevent duplicate usernames
            if (UserStore.userExists(user)) {
                messageLabel.setText("Username already taken.");
                return;
            }
            
            if (deposit < 0) {
                messageLabel.setText("Initial deposit cannot be negative.");
                return;
            }
            // 6. Account Generation: Create a unique ID for the new customer
            String newAccNum = "1000-" + (int)(Math.random() * 9000 + 1000);

            // 7. Persistence: Instantiate BankAccount and store it
            BankAccount newAccount = new BankAccount(
                    newAccNum, 
                    name, 
                    deposit,
                    question, 
                    answer);
            UserStore.addUser(user, pass, newAccount);

            // 8. UX Feedback: Confirm success and clear the form
            messageLabel.setTextFill(Color.GREEN);
            messageLabel.setText("Success! Your Account was created!");
            clearFields();
            

        } catch (NumberFormatException e) {
            // This triggers if the user enters letters or symbols that aren't sanitized
            messageLabel.setText("Invalid deposit amount.");
        }
    }

    /**
     * Transitions the application back to the primary login scene.
     * * @param event The ActionEvent triggered by the "Back" button.
     * @throws IOException If the OnlineBankingView.fxml resource is missing.
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("views/OnlineBankingView.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
    
    /**
     * Helper method to clean and format the user's deposit input.
     * If the input is a valid number, it formats it to include commas, 
     * two decimal places, and a dollar sign (e.g., "5000" becomes "$5,000.00").
     */
    private void formatDepositInput() {
        String input = initialDepositField.getText().trim();
        
        if (input.isEmpty()) return;

        try {
            // Remove any existing formatting symbols first to avoid double-processing
            String cleanInput = input.replaceAll("[,\\$]", "");
            double value = Double.parseDouble(cleanInput);

            // Re-format the string for the UI: %,.2f adds commas and 2 decimals
            String formatted = String.format("$%,.2f", value);
            initialDepositField.setText(formatted);
            
        } catch (NumberFormatException e) {
            // If they typed letters, we don't format it—the handleSignup method
            // will catch this error and show a proper message.
        }
    }
    
    /**
     * Resets all input fields to an empty state after successful registration.
     */
    private void clearFields() {
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        initialDepositField.clear();
        questionField.clear();
        answerField.clear();
    }
    
}
