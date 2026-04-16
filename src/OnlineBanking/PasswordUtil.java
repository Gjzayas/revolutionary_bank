package OnlineBanking;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic security operations.
 * This class provides one-way hashing using the SHA-256 algorithm to ensure 
 * that sensitive data, such as passwords and security answers, are never 
 * stored in plain text within the database.
 * 
 * VERSION HISTORY:
 * 1.0 - Basic utility concept.
 * 2.0 - Added SHA-256 implementation for local serialization.
 * 3.0 - Optimized for MySQL compatibility and standardized verification.
 * 
 * @author: Gabriel J. Zayas
 * Date: 4/06/2026
 * @version 3.0
 */
public class PasswordUtil {

    /**
     * Converts a plain-text string into a secure 64-character SHA-256 hash.
     * 
     * @param password The raw string to be hashed.
     * @return A hexadecimal string representation of the hashed data.
     * @throws RuntimeException If the SHA-256 algorithm is not available in the environment.
     */
    public static String hashPassword(String password) {
        try {
            // Initialize the MessageDigest with the SHA-256 algorithm
            // SHA-256 is a one-way cryptographic hash function
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Convert the input string into bytes and compute the digest (hash)
            byte[] hashedBytes = md.digest(password.getBytes());
            
            // Convert the resulting byte array into a human-readable hexadecimal string
            StringBuilder sb = new StringBuilder();
            
            // Iterate through each byte in the hashed array
            for (byte b : hashedBytes) {
                /*
                 * DECISION/FORMATTING: 
                 * %02x converts the byte to a 2-character hex string.
                 * - '0' ensures a leading zero if the hex value is a single digit.
                 * - '2' sets the width to two characters.
                 * - 'x' specifies lowercase hexadecimal output.
                 */
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        
        } catch (NoSuchAlgorithmException e) {
            // This catch handles the rare case where the JVM doesn't support SHA-256
            throw new RuntimeException("Error: Hashing algorithm not found", e);
        }
    }
    
    /**
     * Verifies if a plain-text string matches a previously stored SHA-256 hash.
     * This is used for both login password checks and security question validation.
     * 
     * @param plainText The raw input provided by the user (e.g., login attempt).
     * @param hashedValue The secure hash retrieved from the MySQL database.
     * @return true if the hash of the input matches the stored hash; false otherwise.
     */
    public static boolean verify(String plainText, String hashedValue) {
        // Hash the new input provided by the user using the same logic
        String hashedInput = hashPassword(plainText);
        
        // Compare the newly generated hash with the one from the database
        return hashedInput.equals(hashedValue);
    }
}