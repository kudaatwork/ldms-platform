package projectlx.user.management.service.tasks.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import projectlx.user.management.service.tasks.api.UserPasswordTask;
import projectlx.user.management.service.utils.dtos.ExpiringPasswordDto;
import projectlx.user.management.service.utils.requests.NotificationRequest;
import projectlx.user.management.service.utils.responses.ExpiringPasswordsResponse;
import projectlx.user.management.service.business.logic.api.UserPasswordService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of UserPasswordTask that handles password expiration notifications
 */
@Service
@RequiredArgsConstructor
public class UserPasswordTaskImpl implements UserPasswordTask {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPasswordTaskImpl.class);
    
    private final UserPasswordService userPasswordService;
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Scheduled job that runs daily at midnight to check for passwords that are about to expire
     * and sends notifications to users through multiple channels (Email, EMS, WhatsApp, InApp)
     */
    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    public void checkForExpiringPasswordsAndNotify() {
        logger.info("Starting scheduled job to check for expiring passwords");
        
        try {
            // Find passwords that will expire in the next 7 days
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sevenDaysFromNow = now.plusDays(7);
            
            ExpiringPasswordsResponse response = userPasswordService.findPasswordsAboutToExpire(now, sevenDaysFromNow);
            
            if (response.isSuccess() && response.getExpiringPasswords() != null) {
                logger.info("Found {} passwords that will expire in the next 7 days", response.getExpiringPasswords().size());
                
                // Send notifications for each expiring password
                for (ExpiringPasswordDto expiringPassword : response.getExpiringPasswords()) {
                    sendPasswordExpiryNotifications(expiringPassword);
                }
                
                logger.info("Completed scheduled job to check for expiring passwords");
            } else {
                logger.warn("Failed to retrieve expiring passwords: {}", response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error in scheduled job to check for expiring passwords: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends password expiry notifications to a user through multiple channels
     * @param expiringPassword The expiring password DTO with user information
     */
    private void sendPasswordExpiryNotifications(ExpiringPasswordDto expiringPassword) {
        logger.info("Sending password expiry notifications to user: {}", expiringPassword.getEmail());
        
        // Calculate days until expiry
        LocalDateTime now = LocalDateTime.now();
        long daysUntilExpiry = java.time.Duration.between(now, expiringPassword.getExpiryDate()).toDays();
        
        // Prepare the dynamic data
        Map<String, Object> data = Map.of(
                "firstName", expiringPassword.getFirstName(),
                "userName", expiringPassword.getUsername(),
                "email", expiringPassword.getEmail(),
                "daysUntilExpiry", daysUntilExpiry,
                "expiryDate", expiringPassword.getExpiryDate().toString()
        );
        
        // Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                expiringPassword.getUserId().toString(),
                expiringPassword.getEmail(),
                expiringPassword.getPhoneNumber(),
                null
        );
        
        // Prepare metadata
        NotificationRequest.Metadata metadata = new NotificationRequest.Metadata(
                "user-management-service",
                UUID.randomUUID().toString()
        );
        
        // Send EMAIL notification
        NotificationRequest emailNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_EXPIRY_NOTIFICATION_EMAIL", // Email template
                recipient,
                data,
                metadata
        );
        
        // Send SMS notification
        NotificationRequest smsNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_EXPIRY_NOTIFICATION_SMS", // SMS template
                recipient,
                data,
                metadata
        );
        
        // Send WhatsApp notification
        NotificationRequest whatsAppNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_EXPIRY_NOTIFICATION_WHATSAPP", // WhatsApp template
                recipient,
                data,
                metadata
        );
        
        // Send IN-APP notification
        NotificationRequest inAppNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_EXPIRY_NOTIFICATION_IN_APP", // In-App template
                recipient,
                data,
                metadata
        );
        
        // Publish all notifications to RabbitMQ
        try {
            // Email notification
            logger.info("Publishing password expiry email notification for user: {}", expiringPassword.getEmail());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", emailNotificationRequest);
            
            // SMS notification
            logger.info("Publishing password expiry SMS notification for user: {}", expiringPassword.getPhoneNumber());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", smsNotificationRequest);
            
            // WhatsApp notification
            logger.info("Publishing password expiry WhatsApp notification for user: {}", expiringPassword.getPhoneNumber());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", whatsAppNotificationRequest);
            
            // In-App notification
            logger.info("Publishing password expiry in-app notification for user: {}", expiringPassword.getUserId());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", inAppNotificationRequest);
            
            logger.info("Successfully published all password expiry notifications for user: {}", expiringPassword.getEmail());
        } catch (Exception e) {
            logger.error("Failed to publish password expiry notifications for user: {}. Error: {}",
                    expiringPassword.getEmail(), e.getMessage(), e);
        }
    }
}