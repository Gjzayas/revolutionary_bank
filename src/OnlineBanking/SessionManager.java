
package OnlineBanking;

/**
 * Manages the lifecycle of a user's active session within the application.
 * This class serves as a global state provider to track the currently logged-in user,
 * ensuring that sensitive data and banking operations are scoped to the correct ID.
 * 
 * @author Gabriel J. Zayas
 * Date: 4/08/2026
 * @version 3.0
 */

public class SessionManager {
    
    /**
     * The unique Primary Key ID of the authenticated user.
     * 
     * LOGIC:
     * - Uses a "Sentinel Value" of -1 to represent a null or inactive state.
     * - This prevents accidental data leakage by ensuring operations fail 
     * unless a valid database ID is explicitly assigned.
     */
    private static int currentUserId = -1; 

    /**
     * Initializes a new session by storing the user's unique identifier.
     * This method is called immediately after a successful database authentication.
     * 
     * @param userId The validated 'id' retrieved from the users database table.
     */
    public static void startSession(int userId) {
        currentUserId = userId;
    }

    /**
     * Terminates the current session and wipes cached user identifiers.
     * 
     * LOGIC & DECISION:
     * - Resets 'currentUserId' to -1, effectively locking the application's 
     * private routes.
     * - This is a critical security step for logout procedures to prevent 
     * the next user from accessing the previous session data.
     */
    public static void clearSession() {
        currentUserId = -1;
        // Future Expansion: Clear user-specific UI preferences or temp balance caches here.
    }

    /**
     * Retrieves the identifier for the user currently using the application.
     * 
     * @return The integer Primary Key of the user; returns -1 if no session is active.
     */
    public static int getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Determines if a valid session is currently established.
     * 
     * DECISION LOGIC:
     * - Performs a comparison check against the sentinel value (-1).
     * - Used by controllers to decide whether to allow access to the Dashboard
     * or redirect the user back to the Login screen.
     * 
     * @return true if 'currentUserId' is a valid database ID; false otherwise.
     */
    public static boolean isActive() {
        return currentUserId != -1;
    }
}
