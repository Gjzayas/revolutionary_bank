
package OnlineBanking;

import java.io.IOException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

/**
 * Controller for the main application dashboard.
 * This class manages the navigation menu, handles page switching within the 
 * main BorderPane, and facilitates dependency injection of the BankAccount 
 * model into sub-controllers.
 * 
 * VERSION HISTORY:
 * 1.0 - Initial GUI with static data.
 * 2.0 - Implementation of Java Object Serialization for local persistence.
 * 3.0 - Migration to MySQL RDBMS; added transaction atomicity and focus-based UI clearing.
 *
 * @author: Gabriel J. Zayas
 * Date: 4/06/2026
 * @version 3.0
 * 
 */
public class DashboardController {
    
    /** Label used to display a personalized greeting to the logged-in user. */
    @FXML private Label welcomeLabel;
    
    /** The root layout container where different sub-views (Overview, Transfer, History) are loaded. */
    @FXML private BorderPane mainPane; 
    
    /** Navigation button to return to the home/overview screen. */
    @FXML private Button homeBtn;
    
    /** Navigation button to open the fund transfer screen. */
    @FXML private Button transferBtn;
    
    /** Navigation button to view the transaction history table. */
    @FXML private Button historyBtn;
    
    /** Navigation button to view and edit user profile settings. */
    @FXML private Button profileBtn;

    /** The data model representing the currently authenticated user's account. */
    private BankAccount userAccount; // The injected Model

    /**
     * Initializes the dashboard with the authenticated user's data.
     * Called by LoginController to inject the model, sync database history, 
     * and set the initial greeting before loading the Overview page.
     * 
     * @param account The BankAccount object belonging to the authenticated user.
     */
    public void setUserAccount(BankAccount account) {
        // Assign the account to the Dashboard's master variable
        this.userAccount = account;
        
        // FETCH fresh transaction history from the database using the User ID
        List<Transaction> dbHistory = UserStore.loadTransactionHistory(userAccount.getId());
        userAccount.getTransactionHistory().clear();
        userAccount.getTransactionHistory().addAll(dbHistory);

        // Logic to extract the First Name from the full name string
        if (userAccount.getFullName() != null && !userAccount.getFullName().isEmpty()) {
            // Split by space and take the first part to keep the greeting friendly
            String firstName = userAccount.getFullName().split(" ")[0];
            welcomeLabel.setText("Welcome, " + firstName + "!");
        
        } else {
            welcomeLabel.setText("Welcome User!");
        }

        // Load the default landing page once the account data is available
        showOverview(); 
    }
    
    /**
     * Manages the visual state of the sidebar navigation.
     * Removes the 'active' CSS class from all buttons and applies it to the selected one.
     * 
     * @param activeBtn The button that was recently clicked by the user.
     */
    private void setActiveButton(Button activeBtn) {
        // Remove the active class from ALL navigation buttons
        homeBtn.getStyleClass().remove("nav-button-active");
        transferBtn.getStyleClass().remove("nav-button-active");
        historyBtn.getStyleClass().remove("nav-button-active");
        profileBtn.getStyleClass().remove("nav-button-active");

        // Add the active class to the one that was just clicked
        if (!activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }

    /**
     * Centralized utility to load FXML sub-views into the center of the main BorderPane.
     * This method handles the logic of loading the file, swapping the view, and 
     * ensuring the sub-controller receives the current userAccount data.
     * 
     * @param fxmlFileName The name of the FXML file (without the .fxml extension) to be loaded.
     */
    private void loadPage(String fxmlFileName) {
        try {
            // REFRESH: Fetch the absolute latest from DB before showing any sub-page
            // This ensures the balance and transaction list are always accurate
            BankAccount latestData = UserStore.findAccountByNumber(userAccount.getAccountNumber());
            
            if (latestData != null) {
                this.userAccount = latestData;
                // Re-sync the transactions list
                this.userAccount.getTransactionHistory().clear();
                this.userAccount.getTransactionHistory().addAll(UserStore.loadTransactionHistory(userAccount.getId()));
            }

            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/" + fxmlFileName + ".fxml"));
            Parent root = loader.load();

            // Inject the refreshed data based on the specific controller type
            Object controller = loader.getController();
            if (controller instanceof OverviewController) {
                ((OverviewController) controller).setAccount(userAccount);
            
            } else if (controller instanceof TransferController) {
                ((TransferController) controller).setAccount(userAccount);
            
            } else if (controller instanceof HistoryController) {
                ((HistoryController) controller).setAccount(userAccount);
            
            } else if (controller instanceof ProfileController) {
                ProfileController profileCtrl = (ProfileController) controller;
                profileCtrl.setUserId(userAccount.getId());
                // Inject the dashboard controller so the profile can talk back to it
                profileCtrl.setParentController(this);
            }

            mainPane.setCenter(root);

        } catch (IOException e) {
            System.err.println("Error: Could not load " + fxmlFileName + ".fxml");
            e.printStackTrace();
        }
    }

    /**
     * Navigates the user to the Overview sub-view and updates button styling.
     */
    @FXML
    private void showOverview() {
        loadPage("Overview");
        setActiveButton(homeBtn);
    }

    /**
     * Navigates the user to the Transfer sub-view and updates button styling.
     */
    @FXML
    private void showTransfer() {
        loadPage("Transfer");
        setActiveButton(transferBtn);
    }

    /**
     * Navigates the user to the Transaction History sub-view and updates button styling.
     */
    @FXML
    private void showHistory() {
        loadPage("History");
        setActiveButton(historyBtn);
    }
    
    /**
     * Navigates the user to the Profile/Settings sub-view and updates button styling.
     */
    @FXML
    private void showProfile() {
        loadPage("Profile");
        setActiveButton(profileBtn);
    }
    
    /**
     * Handles the logout process by clearing the session and returning to the login screen.
     * Safely identifies the current stage regardless of how the method was triggered.
     * 
     * @param event The ActionEvent triggered by the logout button; can be null if called programmatically.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        // Clear the session data
        SessionManager.clearSession();
        
        try {
            // Identify the current application window (Stage) safely
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                // Standard button click path
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            } else {
                stage = (Stage) welcomeLabel.getScene().getWindow(); 
            }

            // Load the primary Login FXML view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/OnlineBankingView.fxml"));
            Parent loginView = loader.load();

            // Configure and display the login scene
            Scene scene = new Scene(loginView);
            stage.setScene(scene);
            stage.setTitle("Revolutionary Bank Login");
            stage.setResizable(false);

            // Center the window on the user's screen
            stage.centerOnScreen();
            stage.show();

            } catch (IOException e) {
                System.err.println("Logout Error: Could not find OnlineBankingView.fxml");
                e.printStackTrace();
        }
    }
    
    /** Updates the welcome label by fetching the latest account name from the database.
     * Specifically used after a user updates their name in the Profile view.
    */
    public void refreshWelcomeMessage() {
       // Fetch latest data to ensure we have the new name
       BankAccount latest = UserStore.findAccountByNumber(userAccount.getAccountNumber());
       
       if (latest != null) {
           this.userAccount = latest;
           
           if (userAccount.getFullName() != null && !userAccount.getFullName().isEmpty()) {
               String firstName = userAccount.getFullName().split(" ")[0];
               welcomeLabel.setText("Welcome, " + firstName + "!");
           }
       }
    }
}
