package projectlx.co.zw.notifications.service.processor.impl;

import projectlx.co.zw.notifications.business.logic.api.PlatformUserNotificationService;
import projectlx.co.zw.notifications.service.processor.api.PlatformUserNotificationProcessor;
import projectlx.co.zw.notifications.utils.responses.PlatformUserNotificationResponse;
import projectlx.co.zw.notifications.utils.support.AuthenticatedUserIdSupport;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;

import java.util.List;
import java.util.Locale;

public class PlatformUserNotificationProcessorImpl implements PlatformUserNotificationProcessor {

    private final PlatformUserNotificationService platformUserNotificationService;
    private final JwtService jwtService;

    public PlatformUserNotificationProcessorImpl(
            PlatformUserNotificationService platformUserNotificationService,
            JwtService jwtService) {
        this.platformUserNotificationService = platformUserNotificationService;
        this.jwtService = jwtService;
    }

    @Override
    public PlatformUserNotificationResponse listInbox(Locale locale, String username) {
        Long userId = AuthenticatedUserIdSupport.resolveUserId(jwtService);
        if (userId == null) {
            PlatformUserNotificationResponse response = new PlatformUserNotificationResponse();
            response.setStatusCode(401);
            response.setSuccess(false);
            response.setMessage("Unable to resolve signed-in user id");
            response.setErrorMessages(List.of("Unable to resolve signed-in user id"));
            return response;
        }
        return platformUserNotificationService.listInbox(userId, locale, username);
    }

    @Override
    public PlatformUserNotificationResponse dismiss(Long notificationId, Locale locale, String username) {
        Long userId = AuthenticatedUserIdSupport.resolveUserId(jwtService);
        if (userId == null) {
            PlatformUserNotificationResponse response = new PlatformUserNotificationResponse();
            response.setStatusCode(401);
            response.setSuccess(false);
            response.setMessage("Unable to resolve signed-in user id");
            response.setErrorMessages(List.of("Unable to resolve signed-in user id"));
            return response;
        }
        return platformUserNotificationService.dismiss(userId, notificationId, locale, username);
    }

    @Override
    public PlatformUserNotificationResponse dismissAll(Locale locale, String username) {
        Long userId = AuthenticatedUserIdSupport.resolveUserId(jwtService);
        if (userId == null) {
            PlatformUserNotificationResponse response = new PlatformUserNotificationResponse();
            response.setStatusCode(401);
            response.setSuccess(false);
            response.setMessage("Unable to resolve signed-in user id");
            response.setErrorMessages(List.of("Unable to resolve signed-in user id"));
            return response;
        }
        return platformUserNotificationService.dismissAll(userId, locale, username);
    }
}
