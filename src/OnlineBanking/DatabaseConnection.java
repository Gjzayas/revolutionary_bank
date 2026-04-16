package OnlineBanking;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connectivity for the Revolutionary Bank application.
 * v3.0 - Decoupled credentials into an external .properties file for security.
 * 
 * @author: Gabriel J. Zayas
 * Date: 4/06/2026
 * @version 3.0
 */
public class DatabaseConnection {
    
    // Variables to hold credentials loaded from the file
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    /**
     * Static initializer block.
     * This runs automatically when the class is referenced for the first time.
     */
    static {
        Properties properties = new Properties();
        // Load the file from the project's resource path
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("resources/db.properties")) {
            
            if (input == null) {
                System.err.println("CRITICAL ERROR: db.properties not found in src folder!");
            } else {
                // Load all key-value pairs from the file
                properties.load(input);
                
                // Map the properties to our class variables
                URL = properties.getProperty("db.url");
                USER = properties.getProperty("db.user");
                PASSWORD = properties.getProperty("db.password");
            }
        } catch (IOException e) {
            System.err.println("FAILED to load database configuration: " + e.getMessage());
        }
    }

    /**
     * Establishes and returns a connection to the MySQL database.
     * @return A valid Connection object, or null if the link fails.
     */
    public static Connection getConnection() {
        try {
            // Registering the driver (Optional in modern JDBC, but good practice)
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Return the connection using the dynamically loaded credentials
            return DriverManager.getConnection(URL, USER, PASSWORD);
            
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("DATABASE CONNECTION ERROR: " + e.getMessage());
            return null;
        }
    }
}
