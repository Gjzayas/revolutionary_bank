
package OnlineBanking;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controller for the Password Recovery/Reset interface.
 * This class manages the multi-step security verification process, allowing users 
 * to recover account access by answering their pre-configured security question 
 * before updating their credentials in the UserStore.
 * 
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 2.0
 * 
 */
public class ResetPasswordController {
    
    /** Input field for the account holder's username or account number. */
    @FXML private TextField usernameField;
    
    /** Input field for the user's answer to the displayed security question. */
    @FXML private TextField answerField;
    
    /** Secure input field for the user to define their new password. */
    @FXML private PasswordField newPasswordField;
    
    /** Label used to display the security question retrieved from the user's account. */
    @FXML private Label questionLabel;
    
    /** Feedback label used to display success or error messages (color-coded for clarity). */
    @FXML private Label messageLabel;

    /**
     * Contacts the UserStore to retrieve the unique security question associated 
     * with the provided username.
     * Provides visual feedback if the account does not exist.
     */
    @FXML
    private void handleFetchQuestion() {
        String username = usernameField.getText();
        String question = UserStore.getQuestionForUser(username);
        
        if (question != null) {
            questionLabel.setText(question);
            messageLabel.setText("");
        
        } else {
            messageLabel.setText("User not found.");
            messageLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Validates the security answer and executes the password update logic.
     * Compares the user input against the stored answer (case-insensitive) 
     * and persists the new password to the data store if successful.
     */
    @FXML
    private void handleReset() {
        String username = usernameField.getText();
        String answer = answerField.getText();
        String newPass = newPasswordField.getText();
        
        // Retrieve the account to verify identity against stored credentials
        BankAccount account = UserStore.findAccountByUsername(username);

        // Security check: Ignore case to improve UX while maintaining security integrity
        if (account != null && account.getSecurityAnswer().equalsIgnoreCase(answer)) {
            UserStore.resetPassword(username, newPass);
            messageLabel.setText("Password updated successfully!");
            messageLabel.setTextFill(Color.GREEN);
        
        } else {
            messageLabel.setText("Incorrect answer. Access denied.");
            messageLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Returns the user to the primary login screen.
     * Includes window centering logic to maintain UI consistency across transitions.
     * * @param event The ActionEvent triggered by the "Back" button.
     * @throws IOException If the OnlineBankingView.fxml file is missing or inaccessible.
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        Parent login = FXMLLoader.load(getClass().getResource("views/OnlineBankingView.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(login));
        stage.centerOnScreen(); // Keep window centered
    }
}
