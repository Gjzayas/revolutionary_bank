package OnlineBanking;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controller for the User Profile interface.
 * This class handles updating personal information, managing security questions, 
 * changing passwords with real-time strength validation, and account deletion.
 * It interacts heavily with the UserStore for database operations and communicates 
 * back to the DashboardController to refresh UI elements.
 * 
 * VERSION HISTORY:
 * 1.0 - Basic UI layout.
 * 2.0 - Added serialization support for updates.
 * 3.0 - MySQL integration, real-time password strength indicator, and match validation.
 * 
 * @author Gabriel J. Zayas
 * Date: 4/06/2026
 * @version 3.0
 */
public class ProfileController {

    // --- UI Fields ---

    /** Input field for updating the user's full name. */
    @FXML private TextField nameField;
    
    /** Input field for updating the user's security question. */
    @FXML private TextField questionField;
    
    /** Input field for updating the user's security answer. */
    @FXML private TextField answerField;
    
    /** Masked field for the user's current password. */
    @FXML private PasswordField currentPassField;
    
    /** Masked field for the new password. */
    @FXML private PasswordField newPassField;
    
    /** Masked field to confirm the new password. */
    @FXML private PasswordField confirmPassField;
    
    /** Label to display success, error, or validation messages to the user. */
    @FXML private Label messageLabel;
    
    /** Label displaying the user's current full name pulled from the database. */
    @FXML private Label displayNameLabel;
    
    /** Label displaying the user's current security question. */
    @FXML private Label displayQuestionLabel;
    
    /** Label displaying a masked version of the security answer. */
    @FXML private Label displayAnswerLabel;
    
    /** Plain-text fields used when the user toggles password visibility to "Show". */
    @FXML private TextField visibleCurrentPass, visibleNewPass, visibleConfirmPass;
    
    /** Buttons to toggle the visibility of the current, new, and confirm password fields. */
    @FXML private Button toggleBtn1, toggleBtn2, toggleBtn3;
    
    /** Visual circular indicator showing password strength or match status. */
    @FXML private ProgressIndicator strengthIndicator;
    
    /** Text label accompanying the strength indicator to describe the current state (e.g., "Weak", "Strong"). */
    @FXML private Label strengthLabel;
    
    // --- State Variables ---

    /** Image asset for the "Show Password" state. */
    private Image openEye;
    
    /** Image asset for the "Hide Password" state. */
    private Image closedEye;

    /** The unique database ID of the currently logged-in user. */
    private int currentUserId;
    
    /** Reference to the parent dashboard controller to allow UI refreshes (e.g., updating the welcome greeting). */
    private DashboardController parentController;
    
    /**
     * Initializes the controller class.
     * Sets up icon images, attaches real-time listeners for the password strength 
     * indicator, and configures an array of fields to clear error messages upon typing.
     */
    @FXML
    public void initialize() {
        // Load eye icons for visibility toggles
        openEye = new Image(getClass().getResource("/OnlineBanking/images/eye_open.png").toExternalForm());
        closedEye = new Image(getClass().getResource("/OnlineBanking/images/eye_shut.png").toExternalForm());
        
        // Initialize all toggle buttons to the "open eye" (hidden password) state
        ((ImageView) toggleBtn1.getGraphic()).setImage(openEye);
        ((ImageView) toggleBtn2.getGraphic()).setImage(openEye);
        ((ImageView) toggleBtn3.getGraphic()).setImage(openEye);
        
        // Listeners: Trigger the strength calculation whenever the new password changes
        newPassField.textProperty().addListener((obs, old, newValue) -> updateStrengthIndicator(newValue));
        visibleNewPass.textProperty().addListener((obs, old, newValue) -> updateStrengthIndicator(newValue));

        // Listeners: Trigger the match check whenever the confirmation password changes
        confirmPassField.textProperty().addListener((obs, old, newValue) -> updateMatchStatus());
        visibleConfirmPass.textProperty().addListener((obs, old, newValue) -> updateMatchStatus());

        // Listeners: Immediately check match status the moment the confirm field gains focus
        confirmPassField.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) updateMatchStatus(); // Check match immediately on focus
        });
        visibleConfirmPass.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) updateMatchStatus();
        });

        // Group all input fields to easily apply the error-clearing listener
        TextField[] allInputs = {
            nameField, questionField, answerField, 
            currentPassField, visibleCurrentPass, 
            newPassField, visibleNewPass, 
            confirmPassField, visibleConfirmPass
        };
        
        // LOOP: Iterate through every input field to attach a listener.
        // DECISION: If the user types anything and an error message is currently displayed, clear the message.
        for (TextField field : allInputs) {
            field.textProperty().addListener((obs, old, newValue) -> {
                if (field.isFocused() && !messageLabel.getText().isEmpty()) {
                    messageLabel.setText("");
                }
            });
        }
    }

    /**
     * Calculates and updates the UI to reflect the strength of the new password.
     * Evaluates length, capitalization, numbers, and special characters.
     * 
     * @param password The current text in the new password field.
     */
    private void updateStrengthIndicator(String password) {
        if (password == null || password.isEmpty()) {
            strengthIndicator.setProgress(0);
            strengthIndicator.setStyle("-fx-progress-color: gray;");
            strengthLabel.setText("Password Strength: Empty Field");
            strengthLabel.setTextFill(Color.SILVER);
            return;
        }
        
        // Score accumulates based on meeting specific complexity criteria
        double score = 0;
        if (password.length() >= 8) score += 0.25;
        if (password.matches(".*[A-Z].*")) score += 0.25; // Contains uppercase
        if (password.matches(".*[0-9].*")) score += 0.25; // Contains number
        if (password.matches(".*[!@#$%^&*()_+=].*")) score += 0.25; // Contains special char

        strengthIndicator.setProgress(score);

        // DECISION: Apply dynamic styling based on the total accumulated score
        if (score <= 0.25) {
            strengthIndicator.setStyle("-fx-progress-color: red;");
            strengthLabel.setText("Password Strength: Weak");
            strengthLabel.setTextFill(Color.RED);
        
        } else if (score <= 0.75) {
            strengthIndicator.setStyle("-fx-progress-color: orange;");
            strengthLabel.setText("Password Strength: Medium");
            strengthLabel.setTextFill(Color.ORANGE);
        
        } else {
            // Maximum score achieved
            strengthIndicator.setStyle("-fx-progress-color: #00FF7F;"); 
            strengthLabel.setText("Password Strength: Strong");
            strengthLabel.setTextFill(Color.web("#00FF7F"));
        }
    }

    /**
     * Compares the text in the "New Password" and "Confirm Password" fields 
     * and updates the UI indicator to reflect whether they match.
     */
    private void updateMatchStatus() {
        // Determine the "New" and "Confirm" values based on visibility
        String newPass = newPassField.isVisible() ? newPassField.getText() : visibleNewPass.getText();
        String confirm = confirmPassField.isVisible() ? confirmPassField.getText() : visibleConfirmPass.getText();

        // DECISION: If the confirm field is cleared out, reset the indicator
        if (confirm.isEmpty()) {
            resetStrengthIndicator(); // Keeps it at "Confirming..." if they backspace everything
            return;
        }

        // DECISION: Check for exact string match
        if (newPass.equals(confirm)) {
            // SUCCESS
            strengthIndicator.setProgress(1.0);
            strengthIndicator.setStyle("-fx-progress-color: #00FF7F;");
            strengthLabel.setText("Passwords Match!");
            strengthLabel.setTextFill(Color.web("#00FF7F"));
        
        } else {
            // PENDING/MISMATCH
            strengthIndicator.setProgress(0.5);
            strengthIndicator.setStyle("-fx-progress-color: orange;");
            strengthLabel.setText("Passwords do not match yet...");
            strengthLabel.setTextFill(Color.ORANGE);
        }
    }

    /**
    * Helper method to swap between masked and plain-text CURRENT password fields.
    */
    @FXML 
    public void handleToggleCurrent() { 
        toggleVisibility(currentPassField, visibleCurrentPass, toggleBtn1); 
    }
    
    /**
    * Helper method to swap between masked and plain-text NEW password fields.
    */
    @FXML 
    public void handleToggleNew() { 
        toggleVisibility(newPassField, visibleNewPass, toggleBtn2); 
    }
    
    /**
    * Helper method to swap between masked and plain-text CONFIRMED password fields.
    */
    @FXML 
    public void handleToggleConfirm() { 
        toggleVisibility(confirmPassField, visibleConfirmPass, toggleBtn3); 
    }

    /**
     * Utility method to swap between masked and plain-text password fields.
     * 
     * @param pass The masked PasswordField.
     * @param visible The plain-text TextField.
     * @param btn The button containing the eye icon to swap.
     */
    private void toggleVisibility(PasswordField pass, TextField visible, Button btn) {
        ImageView icon = (ImageView) btn.getGraphic();
        
        // DECISION: Check which field is currently active and swap to the other
        if (pass.isVisible()) {
            // Switching to visible plain-text
            visible.setText(pass.getText());
            visible.setVisible(true); 
            visible.setManaged(true);
            
            pass.setVisible(false); 
            pass.setManaged(false);
            
            icon.setImage(closedEye);
            
            // UX: Focus and move cursor to end of the text
            visible.requestFocus();
            if (!visible.getText().isEmpty()) {
                visible.positionCaret(visible.getText().length());
            }
        
        } else {
            // Switching back to masked characters
            pass.setText(visible.getText());
            pass.setVisible(true); 
            pass.setManaged(true);
            
            visible.setVisible(false); 
            visible.setManaged(false);
            
            icon.setImage(openEye);
            
            // UX: Focus and move cursor to end of the text
            pass.requestFocus();
            if (!pass.getText().isEmpty()) {
                pass.positionCaret(pass.getText().length());
            }
        }
    }

    /**
     * Injects the user ID into the controller and triggers a data fetch.
     * 
     * @param userId The database ID of the active user.
     */
    public void setUserId(int userId) {
        this.currentUserId = userId;
        loadUserData();
    }

    /**
     * Processes a request to update the user's full name in the database.
     */
    @FXML
    public void handleUpdateName() {
        String newName = nameField.getText().trim();
        
        // VALIDATION GATE: Prevent empty submissions
        if (newName.isEmpty()) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Please provide a Full Name.");
            return; 
        }
        
        // DECISION: Attempt database update and handle success/failure
        if (UserStore.updateUserName(currentUserId, newName)) {
            messageLabel.setTextFill(Color.web("#00FF7F"));
            messageLabel.setText("Name updated successfully!");
            nameField.clear();
            
            // Refresh the labels on screen
            loadUserData(); 
            
            // Notify dashboard to update the "Welcome, Name!" text
            if (parentController != null) {
                parentController.refreshWelcomeMessage();
            }
            
        } else {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Error updating full name.");
        }
    }

    /**
     * Processes a request to update the user's security question and answer.
     */
    @FXML
    public void handleUpdateSecurity() {
        // Acquire Security question and answer from the user
        String q = questionField.getText().trim();
        String a = answerField.getText().trim().toLowerCase();
        
        // VALIDATION GATE: Ensure neither field is left blank
        if (q.isEmpty() || a.isEmpty()) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Please provide a Security question and answer.");
            return;
        }
        
        // SECURITY DECISION: Hash the security answer.
        String hashedAnswer = PasswordUtil.hashPassword(a);
        
        if (UserStore.updateSecurityInfo(currentUserId, q, hashedAnswer)) {
            messageLabel.setTextFill(Color.web("#00FF7F"));
            messageLabel.setText("Security info updated!");
            
            // Clear inputs for security
            questionField.clear();
            answerField.clear();
            
            // Refresh the labels on screen
            loadUserData();
            
        } else {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Error updating security.");
        }
    }

    /**
     * Processes a request to change the user's password.
     * Validates complexity, ensures confirmation matches, and verifies the 
     * old password before committing the change to the database.
     */
    @FXML
    public void handleChangePassword() {
        // Safely extract text depending on which fields are currently visible
        String current = currentPassField.isVisible() ? currentPassField.getText() : visibleCurrentPass.getText();
        String newPass = newPassField.isVisible() ? newPassField.getText() : visibleNewPass.getText();
        String confirm = confirmPassField.isVisible() ? confirmPassField.getText() : visibleConfirmPass.getText();
        
        // VALIDATION GATE: Password Complexity Validation
        // Requires: 8+ chars, 1 digit, 1 uppercase, 1 special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=]).{8,}$";
        
        if (!newPass.matches(passwordPattern)) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Password must be 8+ characters, with 1 uppercase, 1 digit, and 1 special character.");
            return;
        }
        
        // VALIDATION GATE: Confirm field matches new password
        if (!newPass.equals(confirm)) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("New passwords do not match.");
            return;
        }

        // VALIDATION GATE: Verify the provided current password against the database hash
        if (UserStore.verifyPassword(currentUserId, current)) {
            
            // SECURITY DECISION: Hash the validated plain-text password before database entry.
            // Use the SHA-256 utility to ensure no plain-text passwords exist in MySQL.
            String hashedNewPassword = PasswordUtil.hashPassword(newPass);

            // Final Step: Execute the database update
            if (UserStore.updatePassword(currentUserId, hashedNewPassword)) {
                messageLabel.setTextFill(Color.web("#00FF7F"));
                messageLabel.setText("Password changed successfully!");
                
                // Clear all traces of passwords from the UI for security
                currentPassField.clear();
                visibleCurrentPass.clear();
                newPassField.clear();
                visibleNewPass.clear();
                confirmPassField.clear();
                visibleConfirmPass.clear();
                
                // Reset the strength indicator to its default "Empty" state
                updateStrengthIndicator("");
            
            } else {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Error updating password in database.");
            }
        } else {
            // Verification failed
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Current password is incorrect.");
        }
    }
    
    /**
     * Sets the parent dashboard controller to allow for cross-view communication.
     * 
     * @param parent The active DashboardController instance.
     */
    public void setParentController(DashboardController parent) {
            this.parentController = parent;
        }

    /**
     * Fetches the latest user data from the database and updates the display labels.
     */
    private void loadUserData() {
         BankAccount account = UserStore.findAccountById(currentUserId); 

        if (account != null) {
            displayNameLabel.setText("Full Name:\n" + account.getFullName());
            displayQuestionLabel.setText("Security Question:\n" + account.getSecurityQuestion());
            displayAnswerLabel.setText("Security Answer:\n********");
        }
    }
    
    /**
     * Handles the permanent deletion of the user's account.
     * Prompts the user with a final confirmation dialog before executing the 
     * drop cascade in the database and forcing a logout.
     */
    @FXML
    public void handleDeleteAccount() {
        // Configure the alert dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure you want to delete your account?");
        alert.setContentText("This action is permanent and cannot be undone. All your banking data will be lost.");

        // Access the Stage of the alert to set the icon
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();

        // Attempt to load the application icon into the alert window
        try {
            var iconStream = getClass().getResourceAsStream("/OnlineBanking/images/Rev_Logo_resize2.png");
            
            if (iconStream != null) {
                Image brandIcon = new Image(iconStream, 64, 64, true, true);
                alertStage.getIcons().clear();
                alertStage.getIcons().add(brandIcon);
            }
        } catch (Exception e) {
            System.err.println("Note: Alert icon could not be loaded.");
        }

        // DECISION: Check if the user clicked "OK" on the confirmation box
        if (alert.showAndWait().get() == ButtonType.OK) {
            if (UserStore.deleteUser(currentUserId)) {
                // Kick back to login screen via the DashboardController's logout method
                parentController.handleLogout(null);
            
            } else {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Database error: Could not delete account.");
            }
        }
    }
    
    /**
     * Resets the strength indicator to a neutral "Confirming" state.
     * Used when the user focuses on the confirmation field.
     */
    private void resetStrengthIndicator() {
        strengthIndicator.setProgress(0);
        strengthIndicator.setStyle("-fx-progress-color: gray;");
        strengthLabel.setText("Confirming New Password...");
        strengthLabel.setTextFill(Color.SILVER);
    }
}
