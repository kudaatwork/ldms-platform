package projectlx.user.management.service.service.processor.impl;

import projectlx.user.management.service.business.logic.api.UserPasswordService;
import projectlx.user.management.service.service.processor.api.UserPasswordServiceProcessor;
import projectlx.user.management.service.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.service.utils.requests.ResetPasswordRequest;
import projectlx.user.management.service.utils.responses.ExpiringPasswordsResponse;
import projectlx.user.management.service.utils.responses.UserPasswordResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Locale;

@RequiredArgsConstructor
public class UserPasswordServiceProcessorImpl implements UserPasswordServiceProcessor {

    private final UserPasswordService userPasswordService;
    private final Logger logger = LoggerFactory.getLogger(UserPasswordServiceProcessorImpl.class);

    @Override
    public UserPasswordResponse update(ChangeUserPasswordRequest changeUserPasswordRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user password : {}", changeUserPasswordRequest);

        UserPasswordResponse userPasswordResponse = userPasswordService.update(changeUserPasswordRequest, username, locale);

        logger.info("Outgoing response after updating a user password : {}. Status Code: {}. Message: {}",
                userPasswordResponse, userPasswordResponse.getStatusCode(), userPasswordResponse.getMessage());

        return userPasswordResponse;
    }

    @Override
    public UserPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest, Locale locale, String username) {

        logger.info("Incoming request for reset password: {}", resetPasswordRequest);

        UserPasswordResponse userPasswordResponse = userPasswordService.resetPassword(resetPasswordRequest, locale, username);

        logger.info("Outgoing response for reset password: {}. Status Code: {}. Message: {}",
                userPasswordResponse, userPasswordResponse.getStatusCode(), userPasswordResponse.getMessage());

        return userPasswordResponse;
    }
}
