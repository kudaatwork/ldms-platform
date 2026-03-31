package projectlx.user.management.service.tasks.api;

/**
 * Interface for user password related tasks
 */
public interface UserPasswordTask {
    
    /**
     * Checks for passwords that are about to expire and sends notifications to users
     * This method should be scheduled to run periodically
     */
    void checkForExpiringPasswordsAndNotify();
}