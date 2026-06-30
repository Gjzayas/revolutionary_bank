
package OnlineBanking;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.control.TableCell;

/**
 * Controller for the Transaction History sub-view.
 * This class manages the display of the user's transaction ledger using a 
 * formatted TableView. It also provides functionality to export the transaction 
 * history into a professionally formatted official bank statement (TXT file).
 * 
 * @author: Gabriel Zayas
 * Date: 6/16/2026
 * @version 4.0
 * 
 */
public class HistoryController {
    
    /** The main table container for displaying Transaction objects. */
    @FXML private TableView<Transaction> transactionTable;
    
    /** Column for the timestamp of the transaction. */
    @FXML private TableColumn<Transaction, LocalDateTime> colDate;
    
    /** Column for the activity description (e.g., recipient ID). */
    @FXML private TableColumn<Transaction, String> colDescription;
    
    /** Column for the transaction classification (Debit/Credit). */
    @FXML private TableColumn<Transaction, String> colType;
    
    /** Column for the monetary value, featuring dynamic color coding. */
    @FXML private TableColumn<Transaction, Double> colAmount;
    
    /** Column for personal user notes or categories. */
    @FXML private TableColumn<Transaction, String> colNote;

    /** The injected data model containing the user's ledger. */
    private BankAccount account;

    /**
     * Injects the authenticated user's account and initializes the table configuration.
     * 
     * @param account The BankAccount instance belonging to the current user.
     */
    public void setAccount(BankAccount account) {
        this.account = account;
        
        // 1. Fetch the absolute latest from MySQL
        List<Transaction> freshHistory = UserStore.loadTransactionHistory(account.getId());

        // 2. Clear the old account history list and update it with the fresh data
        account.getTransactionHistory().clear();
        account.getTransactionHistory().addAll(freshHistory);

        // 3. Configure the table with the refreshed list
        setupTable();
    }

    /**
     * Configures the TableView columns, including data binding, currency formatting, 
     * and conditional CSS styling for financial entries.
     * 
     * This method uses custom CellFactories to ensure dates look human-readable 
     * and that Debits appear in red (#e74c3c) while Credits appear in green (#27ae60).
     */
    private void setupTable() {
        // Set up the cell factory for the Date column
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        // Link the colNote to the "note" variable in Transaction.java
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        
        // Custom CellFactory to handle empty notes and provide placeholders
        colNote.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null || note.isEmpty()) {
                    setText(null);
                } else {
                    setText(note);
                }
            }
        });
        
        // Create the bank-standard formatter
        DateTimeFormatter bankFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        
        // Custom CellFactory for Date formatting
        colDate.setCellFactory(column -> new TableCell<Transaction, LocalDateTime>() {
        @Override
        protected void updateItem(LocalDateTime item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
            } else {
                // This converts the long timestamp into a clean format "Mar 12, 2026" style
                setText(item.format(bankFormatter));
            }
        }
        });
        
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        // 2. Currency Formatter and Conditional Styling for the Amount Column
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                
                // Reset styles and text
                setStyle("");
                
                // Clear previous styles to prevent "ghosting" when scrolling
                getStyleClass().removeAll("recent-debit", "recent-credit", "text-deposit", "text-withdrawal");
                
                if (empty || amount == null) {
                    setText(null);
                
                } else {
                    // Format the number as currency (e.g., $5,000.00)
                    setText(String.format("$%,.2f", amount));

                    // 1. Get the transaction type from the current row
                    Transaction t = getTableView().getItems().get(getIndex());
                    String type = t.getType(); // "Debit", "Credit", "DEPOSIT"
                    
                    // 2. Check if this is the very first row (the most recent)
                    boolean isMostRecent = (getIndex() == 0);

                    // 3. Apply color based on type
                    if ("Debit".equalsIgnoreCase(type)) {
                        String color = isMostRecent ? "#ff0000" : "#FF4500";
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                        getStyleClass().removeAll("recent-credit", "text-deposit");
                        getStyleClass().add(isMostRecent ? "recent-debit" : "text-withdrawal");
                    
                    } else if ("Credit".equalsIgnoreCase(type) || "DEPOSIT".equalsIgnoreCase(type) || "Loan".equalsIgnoreCase(type)) {
                        String color = isMostRecent ? "#00ff00" : "#00FF7F";
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                        getStyleClass().removeAll("recent-debit", "text-withdrawal");
                        getStyleClass().add(isMostRecent ? "recent-credit" : "text-deposit");
                    } 
                }
            }
        });

        // 3. Binding the data list to the TableView UI
        if (account != null) {
            ObservableList<Transaction> data = FXCollections.observableArrayList(account.getTransactionHistory());
            transactionTable.setItems(data);
        }
    }
    
    /**
     * Opens a system file chooser to allow the user to select a save location 
     * for their official bank statement.
     */
    @FXML
    private void handleGenerateStatement() {
        if (account == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Transaction Statement");
        fileChooser.setInitialFileName("Statement_" + account.getAccountNumber() + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        // Get current stage to show dialog
        Stage stage = (Stage) transactionTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            saveToFile(file);
        }
    }

    /**
     * Writes the transaction history to a text file using professional spacing 
     * and formatting.
     * 
     * @param file The file destination selected by the user.
     */
    private void saveToFile(File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("BANK STATEMENT - OFFICIAL RECORD");
            writer.println("Account Holder: " + account.getFullName());
            writer.println("Account Number: " + account.getAccountNumber());
            writer.println("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("-------------------------------------------------------------------------------------------------------");

            // Format string ensures consistent column alignment in the TXT output
            String format = "%-18s %-25s %-12s %-18s %-20s%n";

            writer.printf(format, "Date", "Description", "Type", "Amount", "Note");
            writer.println("-------------------------------------------------------------------------------------------------------");

            for (Transaction t : account.getTransactionHistory()) {
                writer.printf(format,
                        t.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        t.getDescription(),
                        t.getType(),
                        String.format("$%,.2f", t.getAmount()),
                        (t.getNote() == null || t.getNote().isEmpty()) ? "N/A" : t.getNote());
            }

            writer.println("-------------------------------------------------------------------------------------------------------");
            writer.println("Current Balance: $" + String.format("%,.2f", account.getBalance()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
