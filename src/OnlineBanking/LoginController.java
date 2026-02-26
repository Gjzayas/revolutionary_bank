
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
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/**
 * Controller for the primary Login interface of the Revolutionary Banking app.
 * This class handles user authentication, facilitates the transition to the 
 * dashboard upon successful login, and manages navigation to account recovery 
 * and registration views.
 * * 
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 2.0
 * 
 */
public class LoginController {
    
    /** Input field for the user's account number or username. */
    @FXML
    private TextField usernameField; 

    /** Secure input field for the user's password, masking sensitive characters. */
    @FXML
    private PasswordField passwordField;

    /** Label used to display descriptive error messages for failed login attempts or system issues. */
    @FXML
    private Label errorLabel;

    /**
     * Processes the login request by validating credentials against the UserStore.
     * If authenticated, it performs Dependency Injection by passing the BankAccount 
     * object to the DashboardController before switching the scene.
     * * @param event The ActionEvent triggered by the "Login" button.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        // Basic Validation: Ensure input isn't just whitespace
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Consult the global UserStore for credential verification
        BankAccount authenticatedAccount = UserStore.authenticate(username, password);

        if (authenticatedAccount != null) {
            try {
                // 1. Prepare the FXMLLoader for the Dashboard view
                FXMLLoader loader = new FXMLLoader(getClass().getResource("views/DashboardView.fxml"));
                Parent dashboardRoot = loader.load();

                // 2. DEPENDENCY INJECTION: Retrieve the Dashboard's controller instance
                DashboardController dashController = loader.getController();

                // 3. Pass the authenticated model to the next view before it displays
                dashController.setUserAccount(authenticatedAccount);

                // 4. Handle stage transition and window centering
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(dashboardRoot);
                stage.setScene(scene);
                stage.setTitle("Revolutionary Banking - Dashboard");
                stage.centerOnScreen(); // Nice touch for UX
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
     * * @param event The ActionEvent triggered by the "Sign Up" button.
     * @throws IOException If the SignupView.fxml file cannot be found or loaded.
     */
    @FXML
    private void handleShowSignup(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("views/SignupView.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
    
    /**
     * Navigates the user to the Reset Password/Security Question view.
     * This method resets the window focus and centers the stage for optimal UX.
     * * @param event The ActionEvent triggered by the "Forgot Password" link.
     */
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            // 1. Load the new ResetPasswordView.fxml
            Parent resetRoot = FXMLLoader.load(getClass().getResource("views/ResetPasswordView.fxml"));

            // 2. Get the current Stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Update the stage with the new scene
            stage.setScene(new Scene(resetRoot));

            // 4. Center the window on the monitor
            stage.centerOnScreen();
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading ResetPasswordView: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
