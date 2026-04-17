
package OnlineBanking;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controller for the Password Recovery/Reset interface.
 * This class manages the multi-step security verification process, allowing users 
 * to recover account access by answering their pre-configured security question 
 * before updating their credentials in the UserStore.
 * 
 * VERSION HISTORY:
 * 1.0 - Basic layout and recovery logic.
 * 2.0 - Added real-time strength meter and match validation.
 * 3.0 - Enhanced UI focus listeners, dynamic error clearing, and MySQL integration.
 * 
 * @author Gabriel J. Zayas
 * Date: 4/07/2026
 * @version 3.0
 * 
 */
public class ResetPasswordController {
    // --- UI Fields ---

    /** Input field for the account holder's username. */
    @FXML private TextField usernameField;
    
    /** Input field for the user's answer to the displayed security question. */
    @FXML private TextField answerField;
    
    /** Masked secure input field for the user to define their new password. */
    @FXML private PasswordField newPasswordField;
    
    /** Plain-text field used when the user toggles new password visibility to "Show". */
    @FXML private TextField visiblePasswordField; 
    
    /** Button to toggle visibility of the new password field. */
    @FXML private Button toggleBtn;
    
    /** Label used to display the security question retrieved from the user's account. */
    @FXML private Label questionLabel;
    
    /** Feedback label used to display success, warning, or error messages. */
    @FXML private Label messageLabel;
    
    /** Visual progress bar indicating the cryptographic strength of the new password. */
    @FXML private ProgressBar strengthBar;
    
    /** Text label accompanying the strength bar to describe the current state (e.g., "Weak"). */
    @FXML private Label strengthLabel;
    
    /** Masked secure input field to confirm the new password. */
    @FXML private PasswordField confirmPasswordField;
    
    /** Plain-text field used when the user toggles confirm password visibility to "Show". */
    @FXML private TextField visibleConfirmField;
    
    /** Button to toggle visibility of the confirm password field. */
    @FXML private Button toggleBtn2;
    
    // --- State Variables ---

    /** Image asset representing the "Show Password" state. */
    private Image openEye;
    
    /** Image asset representing the "Hide Password" state. */
    private Image closedEye;
    
    /**
     * Initializes the controller class.
     * Sets up UI graphics, attaches real-time listeners for the password strength meter, 
     * and configures focus listeners to handle dynamic error clearing.
     */
    @FXML
    public void initialize() {
        // Load UI assets
        openEye = new Image(getClass().getResource("images/eye_open.png").toExternalForm());
        closedEye = new Image(getClass().getResource("images/eye_shut.png").toExternalForm());
        
        // Default toggle buttons to "hidden" state
        ((ImageView) toggleBtn.getGraphic()).setImage(openEye);
        ((ImageView) toggleBtn2.getGraphic()).setImage(openEye);
        
        // Listeners: Trigger strength calculation whenever the new password changes
        newPasswordField.textProperty().addListener((obs, old, newValue) -> updateStrengthMeter(newValue));
        visiblePasswordField.textProperty().addListener((obs, old, newValue) -> updateStrengthMeter(newValue));
        
        // Listeners: Trigger match validation whenever the confirmation password changes
        confirmPasswordField.textProperty().addListener((obs, old, newValue) -> updateMatchStatus());
        visibleConfirmField.textProperty().addListener((obs, old, newValue) -> updateMatchStatus());
        
        // Listeners: Check match status the moment the confirm field gains focus
        confirmPasswordField.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) updateMatchStatus();
        });
        visibleConfirmField.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) updateMatchStatus();
        });
        
        // Group all text inputs for the error-clearing loop
        TextField[] allFields = {
            usernameField, answerField, newPasswordField, 
            visiblePasswordField, confirmPasswordField, visibleConfirmField
        };

        // LOOP: Iterate through all text fields to attach custom focus and typing behavior
        for (TextField field : allFields) {
            field.textProperty().addListener((obs, old, newValue) -> {
                // DECISION: Only clear the message label upon typing if it is currently displaying an error or warning.
                // This prevents wiping a green "Success!" message while the clearFields() method is running.
                Color currentColor = (Color) messageLabel.getTextFill();
                
                if (!messageLabel.getText().isEmpty() && !currentColor.equals(Color.web("#00FF7F"))) {
                    messageLabel.setText("");
                }
            });
            
            // DECISION: If a field is clicked/focused, and the current message is a Sea Green success message, 
            // clear it. This prepares the UI for a new interaction.
            field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused && !messageLabel.getText().isEmpty()) {
                    Color currentColor = (Color) messageLabel.getTextFill();
                    if (currentColor.equals(Color.web("#00FF7F"))) {
                        messageLabel.setText("");
                    }
                }
            });
        }
    }

    /**
     * Calculates and visually updates the UI to reflect the strength of the new password.
     * Evaluates length, capitalization, numbers, and special characters.
     * 
     * @param password The current text in the new password field.
     */
    private void updateStrengthMeter(String password) {
        // DECISION: Reset to neutral state if the field is cleared
        if (password == null || password.isEmpty()) {
            strengthBar.setProgress(0);
            strengthBar.setStyle("-fx-accent: gray;");
            strengthLabel.setText("Password Strength: Empty Field");
            return;
        }

        // Score accumulates based on meeting complexity criteria
        double score = 0;
        if (password.length() >= 8) score += 0.25;
        if (password.matches(".*[A-Z].*")) score += 0.25;
        if (password.matches(".*[0-9].*")) score += 0.25;
        if (password.matches(".*[!@#$%^&*()_+=].*")) score += 0.25;

        strengthBar.setProgress(score);

        // DECISION: Apply styling based on accumulated score thresholds
        if (score <= 0.25) {
            strengthBar.setStyle("-fx-accent: red;");
            strengthLabel.setText("Password Strength: Weak");
            strengthLabel.setTextFill(Color.RED);
        
        } else if (score <= 0.75) {
            strengthBar.setStyle("-fx-accent: orange;");
            strengthLabel.setText("Password Strength: Medium");
            strengthLabel.setTextFill(Color.ORANGE);
        
        } else {
            strengthBar.setStyle("-fx-accent: #00FF7F;"); 
            strengthLabel.setText("Password Strength: Strong");
            strengthLabel.setTextFill(Color.web("#00FF7F"));
        }
    }
    
    /**
     * Compares the text in the "New Password" and "Confirm Password" fields 
     * and updates the UI indicator to reflect whether they match.
     */
    private void updateMatchStatus() {
        String pass = newPasswordField.isVisible() ? newPasswordField.getText() : visiblePasswordField.getText();
        String confirm = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : visibleConfirmField.getText();

        // DECISION: Reset state if confirmation is empty
        if (confirm.isEmpty()) {
            resetStrengthMeter();
            return;
        }

        // DECISION: Check for exact string equality
        if (pass.equals(confirm)) {
            strengthBar.setProgress(1.0);
            strengthBar.setStyle("-fx-accent: #00FF7F;");
            strengthLabel.setText("Passwords Match!");
            strengthLabel.setTextFill(Color.web("#00FF7F"));
        
        } else {
            strengthBar.setProgress(0.5);
            strengthBar.setStyle("-fx-accent: orange;");
            strengthLabel.setText("Passwords do not match yet...");
            strengthLabel.setTextFill(Color.ORANGE);
        }
    }

    /**
     * Resets the strength indicator to a neutral state. 
     * Used primarily when the user focuses on an empty confirmation field.
     */
    private void resetStrengthMeter() {
        strengthBar.setProgress(0);
        strengthBar.setStyle("-fx-accent: gray;");
        strengthLabel.setText("Confirming New Password...");
        strengthLabel.setTextFill(Color.SILVER);
    }

    @FXML
    public void handleTogglePassword() {
        toggleVisibility(newPasswordField, visiblePasswordField, toggleBtn);
    }
    
    @FXML
    public void handleToggleConfirm() {
        toggleVisibility(confirmPasswordField, visibleConfirmField, toggleBtn2);
    }

    

    /**
     * Contacts the UserStore to retrieve the unique security question associated 
     * with the provided username. Provides visual feedback if the account does not exist.
     */
    @FXML
    private void handleFetchQuestion() {
        String username = usernameField.getText().trim();
        
        // VALIDATION GATE: Ensure username is provided before querying database
        if (username.isEmpty()) {
            messageLabel.setText("Please enter a username.");
            messageLabel.setTextFill(Color.ORANGE);
            usernameField.requestFocus(); // UX: Move cursor to empty field
            return;
        }
        
        String question = UserStore.getQuestionForUser(username);
        
        // DECISION: Display question if found, otherwise show error
        if (question != null) {
            questionLabel.setText(question);
            messageLabel.setText(""); // Clear previous errors on success
        
        } else {
            messageLabel.setText("User not found.");
            messageLabel.setTextFill(Color.RED);
            questionLabel.setText(""); // Clear any previous question if user isn't found
        }
    }

    /**
     * Validates the security answer and executes the password update logic.
     * Compares the user input against the stored SHA-256 hash and persists 
     * the new password to the data store if verification passes.
     */
    @FXML
    private void handleReset() {
        String username = usernameField.getText().trim();
        String answer = answerField.getText().trim();
        
        // Extract correct password string based on current UI visibility state
        String newPass = newPasswordField.isVisible() ? newPasswordField.getText() : visiblePasswordField.getText();
        String confirmPass = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : visibleConfirmField.getText();
        
        // VALIDATION GATE 1: Mandatory Field Check
        if (username.isEmpty() || answer.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            messageLabel.setTextFill(Color.ORANGE);

            // UX DECISION: Intelligently place cursor in the first empty field encountered
            if (username.isEmpty()) {
                usernameField.requestFocus();
            
            } else if (answer.isEmpty()) {
                answerField.requestFocus();
            
            } else {
                if (newPasswordField.isVisible()) {
                    newPasswordField.requestFocus();
                
                } else {
                    visiblePasswordField.requestFocus();
                }
            }
            return;
        }

        // VALIDATION GATE 2: Confirm Matching
        if (!newPass.equals(confirmPass)) {
            messageLabel.setText("Passwords do not match.");
            messageLabel.setTextFill(Color.RED);
            
            // UX: Focus the password field so the user can try again
            if (newPasswordField.isVisible()) {
                newPasswordField.requestFocus();
            
            } else {
                visiblePasswordField.requestFocus();
            }
            return;
        }
        
        // VALIDATION GATE 3: Password Complexity
        // Requires: 8+ chars, 1 digit, 1 uppercase, 1 special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=]).{8,}$";

        if (!newPass.matches(passwordPattern)) {
            messageLabel.setText("Password must be 8+ characters, with at least 1 uppercase letter, 1 digit, and 1 special character.");
            messageLabel.setTextFill(Color.RED);
            return; // Stop execution here if password is too weak
        }
        
        // Fetch account to verify identity
        BankAccount account = UserStore.findAccountByUsername(username);

        // DECISION: Ensure account exists before checking answers
        if (account != null) {
            
            // Normalize input (case-insensitive checking to prevent frustrating lockouts)
            String normalizedInput = answer.trim().toLowerCase();

            // VERIFICATION GATE: Compare normalized input to stored database hash
            if (PasswordUtil.verify(normalizedInput, account.getSecurityAnswer())) {

                // Success: Hash the new password and update the database
                String hashedPassword = PasswordUtil.hashPassword(newPass);

                // Persist the password change
                UserStore.resetPassword(username, hashedPassword);

                messageLabel.setText("Password updated successfully!");
                messageLabel.setTextFill(Color.web("#00FF7F"));
                clearFields();
            
            } else {
                messageLabel.setText("Incorrect answer. Access denied.");
                messageLabel.setTextFill(Color.RED);
            }
        
        } else {
            messageLabel.setText("User not found.");
            messageLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Returns the user to the primary login screen.
     * Includes window centering logic to maintain UI consistency across transitions.
     * 
     * @param event The ActionEvent triggered by the "Back" button.
     * @throws IOException If the OnlineBankingView.fxml file is missing or inaccessible.
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        Parent login = FXMLLoader.load(getClass().getResource("views/OnlineBankingView.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(login));
        stage.setTitle("Revolutionary Banking - Login");
        stage.centerOnScreen(); // Keep window centered
    }
    
    /**
     * Shared logic for toggling visibility of any password field pair.
     * Handles text transfer, UI component visibility swapping, and caret positioning.
     * * @param pass The masked PasswordField.
     * @param visible The plain-text TextField.
     * @param btn The button containing the eye icon graphics.
     */
    private void toggleVisibility(PasswordField pass, TextField visible, Button btn) {
       ImageView iconView = (ImageView) btn.getGraphic();
       boolean isHidden = pass.isVisible();

       // DECISION: Check current state to determine which way to toggle
       if (isHidden) {
           // Switch to VISIBLE plain-text
           visible.setText(pass.getText());
           visible.setVisible(true); 
           visible.setManaged(true);

           pass.setVisible(false); 
           pass.setManaged(false);

           iconView.setImage(closedEye);

           // UX: Maintain focus and place cursor at end of string
           visible.requestFocus();
           if (!visible.getText().isEmpty()) {
               visible.positionCaret(visible.getText().length());
           }

       } else {
           // Switch to HIDDEN masked text
           pass.setText(visible.getText());
           pass.setVisible(true); 
           pass.setManaged(true);

           visible.setVisible(false); 
           visible.setManaged(false);

           iconView.setImage(openEye);

           pass.requestFocus();
           if (!pass.getText().isEmpty()) {
               pass.positionCaret(pass.getText().length());
           }
       }
    }
   
    /**
     * Utility method to wipe all sensitive text inputs and reset UI indicators.
     * Executed automatically after a successful password reset.
     */
    private void clearFields() {
       // Clear all text input
       usernameField.clear();
       answerField.clear();
       newPasswordField.clear();
       visiblePasswordField.clear();
       confirmPasswordField.clear();
       visibleConfirmField.clear();
       questionLabel.setText("");
      
       // Reset the Progress Bar to Zero
       strengthBar.setProgress(0);
       
       // Reset the Color to "Empty" state (Gray)
       strengthBar.setStyle("-fx-accent: gray;");
       
       // Reset the Label to default "Empty" prompt
       strengthLabel.setText("Password Strength: Empty Field");
       strengthLabel.setTextFill(Color.SILVER);
    }
}
