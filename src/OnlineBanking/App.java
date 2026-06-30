
package OnlineBanking;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The main entry point for the Revolutionary Banking System.
 * This class extends the JavaFX Application class to initialize the primary stage,
 * load the initial FXML layout, and launch the graphical user interface (GUI).
 * 
 * @author: Gabriel Zayas
 * Date: 6/22/2026
 * @version 4.0
 * 
 */
public class App extends Application {
    
    // Instance of the background loan processor
    private final LoanProcessor loanProcessor = new LoanProcessor();
    
    /**
     * The main entry point for all JavaFX applications.
     * This method is called after the system is ready for the application to 
     * begin running on the JavaFX Application Thread.
     * * @param primaryStage The primary window (stage) for this application, 
     * onto which the application scene will be set.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize and Start Background Services
            // Logic: Starts the scheduled thread to poll for pending loans.
            loanProcessor.startService();
            
            // Load the initial Login Screen FXML
            // This initializes the LoginController and the view hierarchy.
            Parent root = FXMLLoader.load(getClass().getResource("views/OnlineBankingView.fxml"));
            
            // Create the Scene
            // The scene acts as the container for all visual content in the root FXML.
            Scene scene = new Scene(root);
                  
            // Configure the Primary Stage
            // Set the window title and disable resizability to ensure the 
            // banking UI maintains its professional, intended layout.
            primaryStage.setTitle("Revolutionary Banking - Login");
            primaryStage.setResizable(false);
            
            // Add bank logo to the window's icon
            var iconStream = getClass().getResourceAsStream("images/Rev_Logo_resize2.png");
            
            if (iconStream != null) {
                // Load with high-quality smoothing at 64x64 for best taskbar/titlebar scaling
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream, 64, 64, true, true));
            }
            
            // Shutdown Logic for Loan processor
            // - Decision: Ensures the background loan thread stops when the user closes the window.
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("[System] Closing application and stopping services...");
                loanProcessor.stopService();
            });
            
            // Attach the Scene to the Stage and reveal the window
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            // Handle critical startup failures, such as missing FXML resources.
            System.err.println("Critical Error: Could not start the application.");
            e.printStackTrace();
            // Ensure processor is stopped if startup fails
            loanProcessor.stopService();
        }
    }

    /**
     * Standard Java main method.
     * This serves as the fallback launch mechanism for the application.
     * * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        // Launches the JavaFX runtime and calls the start() method.
        launch(args);
    }
}
