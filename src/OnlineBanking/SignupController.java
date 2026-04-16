
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
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controller for the account registration/signup interface.
 * This class manages the creation of new bank accounts by validating user input,
 * enforcing SHA-256 password hashing, and persisting new users to the MySQL database.
 * 
 * VERSION HISTORY:
 * 1.0 - Basic registration and local UserStore persistence.
 * 2.0 - Added real-time strength meter and dynamic password toggling.
 * 3.0 - Implemented MySQL integration, regex-based currency sanitization, 
 * and automated transaction logging for initial deposits.
 * 
 * @author Gabriel J. Zayas
 * Date: 4/07/2026
 * @version 3.0
 * 
 */
public class SignupController {
    
    // --- UI Input Fields ---
    
    /** Field for the user's legal full name. */
    @FXML private TextField fullNameField;
    
    /** Field for the desired username; normalized to lowercase for uniqueness. */
    @FXML private TextField usernameField;
    
    /** Field for the user to define their own custom security recovery question. */
    @FXML private TextField questionField;
    
    /** Field for the plain-text answer to the recovery question (hashed before storage). */
    @FXML private TextField answerField;
    
    /** Masked secure field for the primary account password. */
    @FXML private PasswordField passwordField;
    
    /** Masked secure field used to verify password consistency. */
    @FXML private PasswordField confirmPasswordField;
    
    /** Plain-text field used when toggling primary password visibility. */
    @FXML private TextField visiblePasswordField;
    
    /** Plain-text field used when toggling confirmation password visibility. */
    @FXML private TextField visibleConfirmField;
    
    /** Field for the starting balance of the new account. */
    @FXML private TextField initialDepositField;
    
    // --- UI Controls & Indicators ---
    
    /** Button to toggle visibility of the primary password. */
    @FXML private Button toggleBtn1;
    
    /** Button to toggle visibility of the confirmation password. */
    @FXML private Button toggleBtn2;
    
    /** Visual progress bar indicating password complexity. */
    @FXML private ProgressBar strengthBar;
    
    /** Text label providing descriptive feedback for password strength. */
    @FXML private Label strengthLabel;
    
    /** Label used to provide real-time validation and success feedback. */
    @FXML private Label messageLabel;
    
    // --- State & Assets ---
    
    /** Image asset for the "Hide" password state. */
    private Image openEye;
    
    /** Image asset for the "Show" password state. */
    private Image closedEye;
    
    /** * Flag to prevent focus/text listeners from triggering error-clearing logic 
     * while the clearFields() method is programmatically wiping the form. 
     */
    private boolean isCleaning = false;
    
    /**
     * Initializes the controller.
     * Sets up UI assets, attaches real-time validation listeners, and configures 
     * the focus-based currency formatter for the deposit field.
     */
    @FXML
    public void initialize() {
        
        // Listener: Triggers currency formatting whenever the user clicks away from the deposit field
        initialDepositField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // DECISION: newValue is false when focus is lost (blur event - clicks away) from the field
            if (!newValue) { 
                formatDepositInput();
            }
        });
        
        // Resource Loading
        openEye = new Image(getClass().getResource("images/eye_open.png").toExternalForm());
        closedEye = new Image(getClass().getResource("images/eye_shut.png").toExternalForm());
        
        // Initial Graphic Setup
        ((ImageView) toggleBtn1.getGraphic()).setImage(openEye);
        ((ImageView) toggleBtn2.getGraphic()).setImage(openEye);
        
        // Listeners: Real-time strength calculation
        passwordField.textProperty().addListener((obs, old, newValue) -> updateStrengthMeter(newValue));
        visiblePasswordField.textProperty().addListener((obs, old, newValue) -> updateStrengthMeter(newValue));
        
        // Listeners: Real-time match checking
        confirmPasswordField.textProperty().addListener((obs, old, newValue) -> updateMatchStatus());
        visibleConfirmField.textProperty().addListener((obs, old, newValue) -> updateMatchStatus());

        // Listeners: Trigger match status check immediately upon focusing confirmation fields
        confirmPasswordField.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) updateMatchStatus(); 
        });
        visibleConfirmField.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) updateMatchStatus();
        });

        // Group fields for automated listener attachment
        TextField[] allFields = {
            fullNameField, usernameField, questionField, answerField, 
            initialDepositField, passwordField, confirmPasswordField, 
            visiblePasswordField, visibleConfirmField
        };

        // LOOP: Iterate through all inputs to attach error-clearing behavior
        for (TextField field : allFields) {
            field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                
                // DECISION: Clear the message label when the user clicks into a field, 
                // unless the system is currently performing a "Cleaning" operation.
                if (isNowFocused && !isCleaning) {
                    if (!messageLabel.getText().isEmpty()) {
                        messageLabel.setText("");
                    }
                }
            });
            
            // DECISION: Clear messages upon typing to provide a reactive UI
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isCleaning && !messageLabel.getText().isEmpty()) {
                    messageLabel.setText("");
                }
            });
        }
    }

    /**
     * Evaluates password complexity and updates the UI progress bar and color.
     * @param password The string to evaluate.
     */
    private void updateStrengthMeter(String password) {
        if (password == null || password.isEmpty()) {
            strengthBar.setProgress(0);
            strengthBar.setStyle("-fx-accent: gray;");
            strengthLabel.setText("Password Strength: Empty Field");
            return;
        }

        double score = 0;
        if (password.length() >= 8) score += 0.25;
        if (password.matches(".*[A-Z].*")) score += 0.25;
        if (password.matches(".*[0-9].*")) score += 0.25;
        if (password.matches(".*[!@#$%^&*()_+=].*")) score += 0.25;

        strengthBar.setProgress(score);

        // DECISION: Apply color coding based on security thresholds
        if (score <= 0.25) {
            strengthBar.setStyle("-fx-accent: red;");
            strengthLabel.setText("Password Strength: Weak");
            strengthLabel.setTextFill(Color.RED);
        
        } else if (score <= 0.75) {
            strengthBar.setStyle("-fx-accent: orange;");
            strengthLabel.setText("Password Strength: Medium");
            strengthLabel.setTextFill(Color.ORANGE);
        
        } else {
            strengthBar.setStyle("-fx-accent: #00FF7F;"); // Sea Green
            strengthLabel.setText("Password Strength: Strong");
            strengthLabel.setTextFill(Color.web("#00FF7F"));
        }
    }
    
    
    /**
     * Checks if the confirmation field matches the primary password field.
     * Accounts for whether the fields are currently masked or visible.
     */
    private void updateMatchStatus() {
        // Determine the "Primary" and "Confirm" values based on visibility
        String pass = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();
        String confirm = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : visibleConfirmField.getText();

        if (confirm.isEmpty()) {
            resetStrengthMeter(); // Reverts to "Confirming..." if the user clears the field
            return;
        }

        if (pass.equals(confirm)) {
            // SUCCESS: Match found
            strengthBar.setProgress(1.0);
            strengthBar.setStyle("-fx-accent: #00FF7F;"); // Your Sea Green
            strengthLabel.setText("Passwords Match!");
            strengthLabel.setTextFill(Color.web("#00FF7F"));
        } else {
            // PENDING: No match yet
            strengthBar.setProgress(0.5);
            strengthBar.setStyle("-fx-accent: orange;");
            strengthLabel.setText("Passwords do not match yet...");
            strengthLabel.setTextFill(Color.ORANGE);
        }
    }
    
    @FXML
    public void handleTogglePassword1(ActionEvent event) {
        toggleVisibility(passwordField, visiblePasswordField, toggleBtn1);
    }

    @FXML
    public void handleTogglePassword2(ActionEvent event) {
        toggleVisibility(confirmPasswordField, visibleConfirmField, toggleBtn2);
    }

    /**
     * Logic for swapping visibility between masked PasswordFields and plain TextFields.
     * @param pass The masked field.
     * @param visible The plain-text field.
     * @param btn The toggle button.
     */
    private void toggleVisibility(PasswordField pass, TextField visible, Button btn) {
        ImageView iconView = (ImageView) btn.getGraphic();
        boolean isHidden = pass.isVisible();

        if (isHidden) {
            // Switch to VISIBLE
            visible.setText(pass.getText());
            visible.setVisible(true); 
            visible.setManaged(true);
            
            pass.setVisible(false); 
            pass.setManaged(false);
            
            iconView.setImage(closedEye);
            
            // UX: Focus the visible field and move cursor to the end of the field
            visible.requestFocus();
            if (!visible.getText().isEmpty()) {
                visible.positionCaret(visible.getText().length());
            }
        
        } else {
            // Switch to HIDDEN
            pass.setText(visible.getText());
            pass.setVisible(true); 
            pass.setManaged(true);
            
            visible.setVisible(false); 
            visible.setManaged(false);
            
            iconView.setImage(openEye);
            
            // UX: Focus the masked field and move cursor to the end of the field
            pass.requestFocus();
            if (!pass.getText().isEmpty()) {
                pass.positionCaret(pass.getText().length());
            }
        }
    }

    /**
     * Orchestrates the account creation process.
     * Validates input, sanitizes financial data, hashes secrets, and persists to MySQL.
     * 
     * 
     * This method performs:
     * 1. Null/Empty field validation.
     * 2. Password matching verification.
     * 3. Username availability checks via UserStore.
     * 4. Monetary parsing for the initial deposit.
     * 5. Automated generation of a unique account number (Format: 1000-xxxx).
     * 
     * @param event The ActionEvent triggered by the "Sign Up" button.
     */
    @FXML
    private void handleSignup(ActionEvent event) {
        try {
            // Data Gathering
            String name = fullNameField.getText();
            String user = usernameField.getText().trim().toLowerCase();
            String pass = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();
            String confirmPass = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : visibleConfirmField.getText();
            String question = questionField.getText();
            String answer = answerField.getText();
            String depositStr = initialDepositField.getText();

            // VALIDATION GATE 1: Null/Empty Check
            if (name.isEmpty() || user.isEmpty() || pass.isEmpty() || 
                    confirmPass.isEmpty() || depositStr.isEmpty() || 
                    question.isEmpty() || answer.isEmpty()) {
                messageLabel.setText("Please fill in all fields.");
                messageLabel.setTextFill(Color.ORANGE);
                return;
            }
            
            // VALIDATION GATE 2: Password Complexity (Regex)
            String passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=]).{8,}$";
            if (!pass.matches(passwordPattern)) {
                messageLabel.setText("Password must be 8+ characters, with at least 1 uppercase letter, 1 digit, and 1 special character.");
                messageLabel.setTextFill(Color.RED);
                return;
            }
            
            // VALIDATION GATE 3: Equality Check
            if (!pass.equals(confirmPass)) {
                messageLabel.setText("Passwords do not match.");
                return;
            }
            
            // DECISION: SANITIZATION LOGIC
            // Uses Regex to strip formatting symbols ($ and ,) so Double.parseDouble doesn't crash.
            // This replaces any comma (,) or dollar sign ($) with an empty string.
            // Example: "1,250.50" becomes "1250.50"
            String cleanDeposit = depositStr.replaceAll("[,\\$]", "");
            
            // Financial Validation: Parse the cleaned string
            double deposit = Double.parseDouble(cleanDeposit);
            
            // VALIDATION GATE 4: Prevent duplicate usernames
            if (UserStore.userExists(user)) {
                messageLabel.setText("Username already taken.");
                return;
            }
            
            if (deposit < 0) {
                messageLabel.setText("Initial deposit cannot be negative.");
                return;
            }
            
            // Logic: Generate Unique Identifiers
            String newAccNum = "1000-" + (int)(Math.random() * 9000 + 1000);
            
            // SECURITY: Hash the password before saving to DB
            String hashedPassword = PasswordUtil.hashPassword(pass);
            
            // Normalize the answer (trim and lowercase) so matching is consistent
            String normalizedAnswer = answer.trim().toLowerCase();
            String hashedAnswer = PasswordUtil.hashPassword(normalizedAnswer);

            // Persistence: Save to MySQL Database
            int newUserId = UserStore.addUserToDatabase(user, 
                                        name, 
                                        hashedPassword, 
                                        newAccNum, 
                                        deposit, 
                                        question, 
                                        hashedAnswer);
            
            if (newUserId != -1) {
                // LOG THE INITIAL DEPOSIT using the valid user_id
                UserStore.logInitialDeposit(newUserId, deposit);
                
                // UX Feedback: Confirm success and clear the form
                messageLabel.setTextFill(Color.web("#00FF7F"));
                messageLabel.setText("Success! Your Account was created!");
                clearFields();
            
            } else {
                // Database insertion failed
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Error: Could not create account.");
            }

        } catch (NumberFormatException e) {
            // This triggers if the user enters letters or symbols that aren't sanitized
            messageLabel.setText("Invalid deposit amount.");
        }
    }

    /**
     * Transitions the application back to the login scene.
     * @param event The ActionEvent.
     * @throws IOException If FXML loading fails.
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        // Load the primary Login FXML
        Parent root = FXMLLoader.load(getClass().getResource("views/OnlineBankingView.fxml"));

        // Access the current stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Set the scene and update the title
        stage.setScene(new Scene(root));
        stage.setTitle("Revolutionary Banking - Login");

        // Re-center the window to account for the smaller login dimensions
        stage.centerOnScreen();
        stage.show();
    }
    
    /**
     * Formats the deposit input into a standard currency string.
     * Uses Regex sanitization before applying String.format.
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
            // Silently fail here; handleSignup will catch it on submit
        }
    }
    
    /**
     * Clears all fields and resets the UI state.
     * Uses the 'isCleaning' flag to silence automated message clearing listeners.
     */
    private void clearFields() {
        // Silence the "Clear Message" listeners
        isCleaning = true; 
        
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        visiblePasswordField.clear();
        confirmPasswordField.clear();
        visibleConfirmField.clear();
        initialDepositField.clear();
        questionField.clear();
        answerField.clear();
        
        // Reset the strength meter for the next use
        strengthBar.setProgress(0);
        strengthLabel.setText("Password Strength: Empty Field");
        strengthLabel.setTextFill(Color.SILVER);
        
        // Re-enable listeners for manual user typing
        isCleaning = false; 
    }
    
    /**
    * Resets the strength bar and label to their neutral/initial state.
    */
    private void resetStrengthMeter() {
        strengthBar.setProgress(0);
        strengthBar.setStyle("-fx-accent: gray;");
        strengthLabel.setText("Confirming Password...");
        strengthLabel.setTextFill(Color.SILVER);
    }
}
