
package OnlineBanking;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the Overview sub-view within the Dashboard.
 * This class is responsible for displaying the primary account details,
 * specifically the current available balance and the unique account number,
 * formatted for a professional banking user interface.
 * 
 * @author: Gabriel Zayas
 * Date: 6/22/2026
 * @version 4.0
 * 
 */
public class OverviewController {
    
    /** Label that displays the current balance, formatted with currency symbols and commas. */
    @FXML private Label balanceLabel;
    
    /** Label that displays the unique identification number of the user's account. */
    @FXML private Label accountNumberLabel;

    /** The injected data model containing the user's current financial status. */
    private BankAccount account;

    /**
     * Automatically called by JavaFX after the FXML file has been loaded.
     * Ensures that the UI attempt an initial refresh as soon as the view is ready.
     */
    @FXML
    public void initialize() {
        updateUI(); 
    }
    
    /**
     * Injects the authenticated user's account into this controller and updates the UI.
     * This method is called by the DashboardController's loadPage utility.
     * 
     * @param account The BankAccount instance belonging to the current user.
     */
    public void setAccount(BankAccount account) {
        this.account = account;
        updateUI();
    }

    /**
     * Refreshes the display labels with the most recent data from the account model.
     * This method handles:
     * 1. Currency formatting (adding commas and dollar signs).
     * 2. Dynamic CSS styling (applying 'Banking Green' to the balance).
     * 3. Error handling for missing FXML IDs or null account references.
     */
    private void updateUI() {
        // Validation check to ensure SceneBuilder fx:id is properly linked
        if (balanceLabel == null) {
            System.err.println("Error: balanceLabel is NULL. Check fx:id in SceneBuilder.");
            return;
        }

        if (account != null) {
            // 1. Format the balance with thousands separators and two decimal places
            // The comma after the % adds the thousands separator
            balanceLabel.setText(String.format("$%,.2f", account.getBalance()));
            
            // 2. Apply branding: Set the text to a professional green color (#27ae60) and bold font
            balanceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            
            // 3. Update the account number label if it exists in the FXML
            if (accountNumberLabel != null) {
                accountNumberLabel.setText("Account: " + account.getAccountNumber());
            
            } else {
                // Default state if no account data is injected
                balanceLabel.setText("$0.00");
                balanceLabel.setStyle("-fx-text-fill: black;"); // Reset color if no account
                
                if (accountNumberLabel != null) {
                    accountNumberLabel.setText("Account: N/A");
                }
            }   
        }
    }
    
    /**
    * Refreshes the account data directly from the database and updates the UI.
    * This ensures payments made in the Loan center are reflected here.
    */
   public void refreshBalance() {
       if (account != null) {
           // Fetch the fresh balance from UserStore using the stored account's user ID
           double freshBalance = UserStore.getAccountBalance(account.getId(), "Checking");

           // Update the local model so it stays in sync
           account.setBalance(freshBalance);

           // Push the new data to the labels
           updateUI();
       }
   }
}
