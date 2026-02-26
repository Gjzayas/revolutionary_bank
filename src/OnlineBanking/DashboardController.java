
package OnlineBanking;

import java.io.IOException;
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
 * @author: Gabriel Zayas
 * Date: 2/20/2026
 * @version 2.0
 * 
 */
public class DashboardController {
    
    /** Label used to display a personalized greeting to the logged-in user. */
    @FXML private Label welcomeLabel;
    
    /** The root layout container where different sub-views (Overview, Transfer, History) are loaded. */
    @FXML private BorderPane mainPane; // The fx:id of root BorderPane in SceneBuilder
    
    @FXML private Button homeBtn;
    @FXML private Button transferBtn;
    @FXML private Button historyBtn;

    /** The data model representing the currently authenticated user's account. */
    private BankAccount userAccount; // The injected Model

    /**
     * Initializes the dashboard with the authenticated user's data.
     * This method is called by the LoginController to "inject" the account model,
     * personalize the UI with the user's first name, and load the initial Overview page.
     * * @param account The BankAccount object belonging to the authenticated user.
     */
    public void setUserAccount(BankAccount account) {
        // 1. Assign the account to the Dashboard's master variable FIRST
        this.userAccount = account;
        
        // 2. Logic to extract the First Name from the full name string
        if (userAccount.getFullName() != null && !userAccount.getFullName().isEmpty()) {
            
            // Split by space and take the first part to keep the greeting friendly
            String firstName = userAccount.getFullName().split(" ")[0];
            welcomeLabel.setText("Welcome, " + firstName + "!");
        
        } else {
            welcomeLabel.setText("Welcome User!");
        }
        
        // 3. Load the default landing page once the account data is available
        showOverview(); 
    }
    
    /**
     * 
     * This "cleans" the buttons so only one is highlighted at a time.
     */
    private void setActiveButton(Button activeBtn) {
        // Remove the active class from ALL navigation buttons
        homeBtn.getStyleClass().remove("nav-button-active");
        transferBtn.getStyleClass().remove("nav-button-active");
        historyBtn.getStyleClass().remove("nav-button-active");

        // Add the active class to the one that was just clicked
        if (!activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }

    /**
     * Centralized utility to load FXML sub-views into the center of the main BorderPane.
     * This method handles the logic of loading the file, swapping the view, and 
     * ensuring the sub-controller receives the current userAccount data.
     * * @param fxmlFileName The name of the FXML file (without the .fxml extension) to be loaded.
     */
    private void loadPage(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/" + fxmlFileName + ".fxml"));
            Parent root = loader.load();

            // Dependency Injection Logic:
            // Retrieve the controller instance created by the FXMLLoader
            Object controller = loader.getController();

            // Pass the account data to the specific controller
            if (controller instanceof OverviewController) {
                ((OverviewController) controller).setAccount(userAccount);
            
            } else if (controller instanceof TransferController) {
                ((TransferController) controller).setAccount(userAccount);
            
            } else if (controller instanceof HistoryController) {
                ((HistoryController) controller).setAccount(userAccount);
            }

            // Swap the center of the BorderPane with the newly loaded root node
            mainPane.setCenter(root);

        } catch (IOException e) {
            System.err.println("Error: Could not load " + fxmlFileName + ".fxml");
            e.printStackTrace();
        }
    }

    /**
     * Navigates the user to the Overview sub-view.
     */
    @FXML
    private void showOverview() {
        loadPage("Overview");
        setActiveButton(homeBtn);
    }

    /**
     * Navigates the user to the Transfer sub-view.
     */
    @FXML
    private void showTransfer() {
        loadPage("Transfer");
        setActiveButton(transferBtn);
    }

    /**
     * Navigates the user to the Transaction History sub-view.
     */
    @FXML
    private void showHistory() {
        loadPage("History");
        setActiveButton(historyBtn);
    }
    
    /**
     * Handles the logout process by returning the user to the primary login screen.
     * This method clears the current dashboard stage and re-initializes the login view.
     * * @param event The ActionEvent triggered by clicking the logout button.
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
        // 1. Load the primary Login FXML view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("views/OnlineBankingView.fxml"));
        Parent loginView = loader.load();

        // 2. Identify the current application window (Stage)
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // 3. Configure and display the login scene
        Scene scene = new Scene(loginView);
        stage.setScene(scene);
        stage.setTitle("Revolutionary Bank Login");
        stage.setResizable(false);
        
        // 4. Center the window on the user's screen for better UX
        stage.centerOnScreen();
        
        stage.show();

        } catch (IOException e) {
            System.err.println("Logout Error: Could not find OnlineBankingView.fxml");
            e.printStackTrace();
        }
    }
}
