
package OnlineBanking;

import javafx.scene.control.Label;       
import javafx.scene.control.TextField;   
import javafx.event.ActionEvent;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.util.prefs.Preferences; 
import javafx.scene.control.CheckBox;

/**
 * Controller for the primary Login interface of the Revolutionary Banking app.
 * This class handles user authentication via MySQL, manages "Remember Me" 
 * preferences, and facilitates scene transitions to the dashboard or recovery views.
 * 
 * VERSION HISTORY:
 * 1.0 - Initial GUI.
 * 2.0 - Local Serialization support.
 * 3.0 - MySQL Authentication and password visibility toggling.
 * 
 * @author: Gabriel J. Zayas
 * Date: 4/06/2026
 * @version 3.0
 * 
 */
public class LoginController {
    
    // --- UI Fields ---
    
    /** Input field for the user's account number or username. */
    @FXML
    private TextField usernameField; 

    /** Secure input field for the user's password, masking sensitive characters. */
    @FXML
    private PasswordField passwordField;
    
    /** Label used to display descriptive error messages for failed login attempts or system issues. */
    @FXML private Label errorLabel;
    
    /** Secondary text field used to show the password in plain text when toggled. */
    @FXML private TextField visiblePasswordField;
    
    /** Button that triggers the password visibility toggle. */
    @FXML private Button toggleBtn;
    
    /** Checkbox to save the username locally using Java Preferences API. */
    @FXML private CheckBox rememberMeCheckbox;
    
    /** The main login submission button. */
    @FXML private Button loginButton;

    // --- State Variables ---
    
    /** Image representing the "Show Password" state. */
    private Image openEye;
    
    /** Image representing the "Hide Password" state. */
    private Image closedEye;
    
    /** Preferences node used to store the 'Remember Me' username on the local machine. */
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
    
    /** Constant key for the remembered username preference. */
    private static final String REMEMBERED_USER = "remembered_username";

    /**
     * Initializes the controller class. 
     * Loads UI icons, attaches error-clearing listeners to input fields, 
     * and auto-fills the username if the 'Remember Me' preference is set.
     */
    @FXML
    public void initialize() {
        // Load the images from the images resource folder
        openEye = new Image(getClass().getResource("images/eye_open.png").toExternalForm());
        closedEye = new Image(getClass().getResource("images/eye_shut.png").toExternalForm());
        
        // Set the initial icon (Open Eye)
        ImageView iconView = (ImageView) toggleBtn.getGraphic();
        iconView.setImage(openEye);
        
        // Error Clearing Listeners
        // Attach listeners to clear error messages as soon as the user interacts with any fields
        usernameField.textProperty().addListener((obs, old, newValue) -> errorLabel.setText(""));
        passwordField.textProperty().addListener((obs, old, newValue) -> errorLabel.setText(""));
        visiblePasswordField.textProperty().addListener((obs, old, newValue) -> errorLabel.setText(""));
        
        // Check for a previously saved username
        String savedUser = prefs.get(REMEMBERED_USER, "");
        if (!savedUser.isEmpty()) {
            usernameField.setText(savedUser);
            rememberMeCheckbox.setSelected(true);
            
            // Redirect focus to the password field so the username field isn't highlighted in blue
            javafx.application.Platform.runLater(() -> {
                passwordField.requestFocus(); 
            });
        }
    }

    /**
     * Processes the login request by validating credentials against the database.
     * If authenticated, it injects the BankAccount model into the DashboardController 
     * and transitions the scene.
     * 
     * @param event The ActionEvent triggered by clicking the Login button.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        // Extract and clean user input
        String username = usernameField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();
        
        // Authenticate via the MySQL UserStore
        BankAccount authenticatedAccount = UserStore.authenticate(username, password);

        if (authenticatedAccount != null) {
            // Update 'Remember Me' preferences based on checkbox state
            if (rememberMeCheckbox.isSelected()) {
                prefs.put(REMEMBERED_USER, username);
            
            } else {
                prefs.remove(REMEMBERED_USER); // Clear if unchecked
            }
            
            try {
                // Load the Dashboard FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("views/DashboardView.fxml"));
                Parent dashboardRoot = loader.load();

                // Inject the authenticated model into the DashboardController
                DashboardController dashController = loader.getController();

                // Pass the authenticated model to the next view before it displays
                dashController.setUserAccount(authenticatedAccount);

                // Finalize the stage transition and window centering
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(dashboardRoot);
                stage.setScene(scene);
                stage.setTitle("Revolutionary Banking - Dashboard");
                stage.centerOnScreen(); 
                stage.show();

            } catch (IOException e) {
                errorLabel.setText("System Error: Could not load Dashboard.");
                e.printStackTrace();
            }
        } else {
            errorLabel.setText("Invalid credentials. Please Try Again.");
        }
    }
    
    /**
     * Navigates the user to the Signup/Registration view.
     * 
     * @param event The ActionEvent triggered by the "Sign Up" button.
     * @throws IOException If the SignupView.fxml file cannot be found or loaded.
     */
    @FXML
    private void handleShowSignup(ActionEvent event) throws IOException {
        // Load the SignupView FXML
        Parent root = FXMLLoader.load(getClass().getResource("views/SignupView.fxml"));
        
        // Retrieve the current Stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
        // Set the new Scene
        stage.setScene(new Scene(root));
        
        // Update Title and Center window
        stage.setTitle("Revolutionary Banking - Create New Account");
        stage.centerOnScreen(); 

        stage.show();
    }
    
    /**
     * Navigates the user to the Reset Password/Security Question view.
     * 
     * @param event The ActionEvent triggered by the "Forgot Password" link.
     */
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            // Load the new ResetPasswordView.fxml
            Parent resetRoot = FXMLLoader.load(getClass().getResource("views/ResetPasswordView.fxml"));

            // Get the current Stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Update the stage with the new scene
            stage.setScene(new Scene(resetRoot));
            stage.setTitle("Revolutionary Banking - Forgot Password");

            // Center the window on the monitor
            stage.centerOnScreen();
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading ResetPasswordView: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Toggles between masked (PasswordField) and plain-text (TextField) views.
     * Synchronizes the text between both fields and swaps the eye icon.
     */
    @FXML
    public void handleTogglePassword() {
        ImageView iconView = (ImageView) toggleBtn.getGraphic();
        boolean isVisible = visiblePasswordField.isVisible();

        if (!isVisible) {
            // Logic to switch from Hidden -> Visible
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            
            iconView.setImage(closedEye); // Show "Hide" icon
            
            // FOCUS: Return cursor to the visible text field
            visiblePasswordField.requestFocus();
            // Move cursor to the end of the text
            visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
        
        } else {
            // Logic to switch from Visible -> Hidden
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            
            iconView.setImage(openEye); // Show "Show" icon
            
            // FOCUS: Return cursor to the masked password field
            passwordField.requestFocus();
            // Move cursor to the end of the text
            passwordField.positionCaret(passwordField.getText().length());
        }
    }
    
}
