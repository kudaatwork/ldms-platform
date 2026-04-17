package projectlx.user.management.service.business.logic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.business.logic.api.UserPasswordService;
import projectlx.user.management.service.business.validator.api.UserPasswordServiceValidator;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserPassword;
import projectlx.user.management.service.repository.UserPasswordRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.utils.dtos.ExpiringPasswordDto;
import projectlx.user.management.service.utils.dtos.UserPasswordDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.service.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.service.utils.requests.NotificationRequest;
import projectlx.user.management.service.utils.requests.ResetPasswordRequest;
import projectlx.user.management.service.utils.responses.ExpiringPasswordsResponse;
import projectlx.user.management.service.utils.responses.UserPasswordResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isPasswordValid;

@Data
@RequiredArgsConstructor
@EnableScheduling
public class UserPasswordServiceImpl implements UserPasswordService {

    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final UserPasswordRepository userPasswordRepository;
    private final UserRepository userRepository;
    private final UserPasswordServiceValidator userPasswordServiceValidator;
    private final UserPasswordServiceAuditable userPasswordServiceAuditable;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserServiceAuditable userServiceAuditable; // For updating user entity
    private final RabbitTemplate rabbitTemplate; // For sending emails

    private static final Logger logger = LoggerFactory.getLogger(UserPasswordServiceImpl.class);

    @Override
    public UserPasswordResponse create(CreateUserPasswordRequest createUserPasswordRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PASSWORD_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserPasswordResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(createUserPasswordRequest.getUserId(),
                EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserPasswordResponse(404, false, message, null, null,
                    null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserPassword userPasswordToBeSaved = modelMapper.map(createUserPasswordRequest, UserPassword.class);
        userPasswordToBeSaved.setUser(userRetrieved.get());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime passwordExpiryDate = now.plusDays(90);

        userPasswordToBeSaved.setPassword(bCryptPasswordEncoder.encode(createUserPasswordRequest.getPassword()));
        userPasswordToBeSaved.setExpiryDate(passwordExpiryDate);
        userPasswordToBeSaved.setIsPasswordExpired(false);

        UserPassword userPasswordSaved = userPasswordServiceAuditable.create(userPasswordToBeSaved, locale, username);

        UserPasswordDto userPasswordDtoReturned = modelMapper.map(userPasswordSaved, UserPasswordDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserPasswordResponse(201, true, message, userPasswordDtoReturned, null,
                null);
    }

    @Override
    public UserPasswordResponse findById(Long id, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userPasswordServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserPasswordResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserPassword> userPasswordRetrieved = userPasswordRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userPasswordRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserPasswordResponse(404, false, message, null, null,
                    null);
        }

        UserPassword userPasswordReturned = userPasswordRetrieved .get();
        UserPasswordDto userPasswordDto = modelMapper.map(userPasswordReturned, UserPasswordDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserPasswordResponse(200, true, message, userPasswordDto, null,
                null);
    }

    @Override
    public UserPasswordResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserPassword> userPasswordList = userPasswordRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userPasswordList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserPasswordResponse(404, false, message, null,
                    null, null);
        }

        List<UserPasswordDto> userPasswordDtoList = modelMapper.map(userPasswordList, new TypeToken<List<UserPasswordDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserPasswordResponse(200, true, message, null, userPasswordDtoList,
                null);
    }

    @Override
    public UserPasswordResponse update(ChangeUserPasswordRequest changeUserPasswordRequest, String username, Locale locale) {

        String message = "";

        // Validate the request
        ValidatorDto validatorDto = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PASSWORD_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildUserPasswordResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // APPROACH 1: Find by User ID (Recommended)
        Optional<UserPassword> userPasswordOptional = userPasswordRepository.findByUserIdAndEntityStatusNot(
                changeUserPasswordRequest.getUserId(), EntityStatus.DELETED);

        if (userPasswordOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildUserPasswordResponse(404, false, message, null, null, null);
        }

        UserPassword userPasswordToBeEdited = userPasswordOptional.get();

        // OPTIONAL: Verify current password (for security)
        if (changeUserPasswordRequest.getOldPassword() != null &&
                !changeUserPasswordRequest.getOldPassword().trim().isEmpty()) {

            if (!bCryptPasswordEncoder.matches(changeUserPasswordRequest.getOldPassword(),
                    userPasswordToBeEdited.getPassword())) {

                message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_INVALID.getCode(),
                        new String[]{}, locale);

                List<String> errors = new ArrayList<>();
                errors.add(message);
                return buildUserPasswordResponseWithErrors(400, false, message, null, null, errors);
            }
        }

        // UPDATE THE ACTUAL PASSWORD (This was missing!)
        userPasswordToBeEdited.setPassword(bCryptPasswordEncoder.encode(changeUserPasswordRequest.getPassword()));

        // Update expiry date (90 days from now)
        LocalDateTime passwordExpiryDate = LocalDateTime.now().plusDays(90);
        userPasswordToBeEdited.setExpiryDate(passwordExpiryDate);
        userPasswordToBeEdited.setIsPasswordExpired(false);

        // Save the updated password
        UserPassword userPasswordEdited = userPasswordServiceAuditable.update(userPasswordToBeEdited, locale, username);

        // Map to DTO
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserPasswordDto userPasswordDtoReturned = modelMapper.map(userPasswordEdited, UserPasswordDto.class);

        // Get the user associated with this password and send notification
        Optional<User> userOptional = userRepository.findByIdAndEntityStatusNot(
                changeUserPasswordRequest.getUserId(), EntityStatus.DELETED);
        
        if (userOptional.isPresent()) {
            // Send notification about password change
            sendPasswordChangeConfirmationEmail(userOptional.get());
        } else {
            logger.warn("Could not send password change notification: User with ID {} not found", 
                    changeUserPasswordRequest.getUserId());
        }

        message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserPasswordResponse(200, true, message, userPasswordDtoReturned, null, null);
    }

    @Override
    public UserPasswordResponse delete(Long id, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userPasswordServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserPasswordResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserPassword> userPasswordRetrieved = userPasswordRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userPasswordRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserPasswordResponse(404, false, message, null, null,
                    null);
        }

        UserPassword userPasswordToBeDeleted = userPasswordRetrieved.get();
        userPasswordToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserPassword userPasswordDeleted = userPasswordServiceAuditable.delete(userPasswordToBeDeleted, locale);

        UserPasswordDto userPasswordDtoReturned = modelMapper.map(userPasswordDeleted, UserPasswordDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserPasswordResponse(200, true, message, userPasswordDtoReturned, null,
                null);
    }

    @Override
    public UserPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest, Locale locale, String username) {

        String message;

        // Validate input
        ValidatorDto validatorDto = validateResetPasswordRequest(resetPasswordRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_RESET_PASSWORD_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserPasswordResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        // Check if passwords match
        if (!resetPasswordRequest.getNewPassword().equals(resetPasswordRequest.getConfirmPassword())) {

            message = messageService.getMessage(I18Code.MESSAGE_PASSWORDS_DO_NOT_MATCH.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);

            return buildUserPasswordResponseWithErrors(400, false, message, null, null, errors);
        }

        // Validate password strength
        if (!isPasswordValid(resetPasswordRequest.getNewPassword())) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_INVALID.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);

            return buildUserPasswordResponseWithErrors(400, false, message, null,
                    null, errors);
        }

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmailAndEntityStatusNot(
                resetPasswordRequest.getEmail(), EntityStatus.DELETED);

        if (userOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserPasswordResponse(404, false, message, null,
                    null, null);
        }

        User user = userOptional.get();

        // Verify the token matches and hasn't expired
        if (user.getPasswordResetToken() == null ||
                !user.getPasswordResetToken().equals(resetPasswordRequest.getToken()) ||
                user.getPasswordResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {

            message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_INVALID.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);

            return buildUserPasswordResponseWithErrors(400, false, message, null,
                    null, errors);
        }

        // Update password using existing update method
        ChangeUserPasswordRequest changePasswordRequest = new ChangeUserPasswordRequest();
        changePasswordRequest.setUserId(user.getId());
        changePasswordRequest.setPassword(resetPasswordRequest.getNewPassword());

        UserPasswordResponse passwordResponse = this.update(changePasswordRequest, username, locale);

        if (!passwordResponse.isSuccess()) {
            return passwordResponse; // Return the error response as-is
        }

        // Clear reset token after successful password change - HANDLE DIRECTLY
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userServiceAuditable.update(user, locale, username); // Use injected UserServiceAuditable

        // Send password change confirmation email - HANDLE DIRECTLY
        sendPasswordChangeConfirmationEmail(user);

        message = messageService.getMessage(I18Code.MESSAGE_PASSWORD_RESET_SUCCESSFUL.getCode(),
                new String[]{}, locale);

        return buildUserPasswordResponse(200, true, message, passwordResponse.getUserPasswordDto(),
                null, null);
    }

    private ValidatorDto validateResetPasswordRequest(ResetPasswordRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_RESET_PASSWORD_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_EMAIL_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PASSWORD_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CONFIRM_PASSWORD_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    /**
     * Enhanced sendPasswordChangeConfirmationEmail with multi-channel notifications
     */
    private void sendPasswordChangeConfirmationEmail(User user) {

        // Prepare the dynamic data
        Map<String, Object> data = Map.of(
                "firstName", user.getFirstName(),
                "userName", user.getUsername(),
                "Email", user.getEmail()
        );

        // Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId().toString(),
                user.getEmail(),
                user.getPhoneNumber(),
                null
        );

        // Send EMAIL confirmation
        NotificationRequest emailNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_CHANGE_CONFIRMATION", // Email template
                recipient,
                data,
                null
        );

        // Send SMS confirmation
        NotificationRequest smsNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_CHANGE_CONFIRMATION_SMS", // SMS template
                recipient,
                data,
                null
        );

        // Send IN-APP confirmation
        NotificationRequest inAppNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_CHANGE_IN_APP", // In-App template
                recipient,
                data,
                null
        );

        // Publish all notifications to RabbitMQ
        try {
            // Email confirmation
            logger.info("Publishing password change confirmation email for user: {}", user.getEmail());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", emailNotificationRequest);
            logger.info("Successfully published password change confirmation email for user: {}", user.getEmail());

            // SMS confirmation (security alert)
            logger.info("Publishing password change confirmation SMS for user: {}", user.getPhoneNumber());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", smsNotificationRequest);
            logger.info("Successfully published password change confirmation SMS for user: {}", user.getPhoneNumber());

            // In-App confirmation
            logger.info("Publishing password change confirmation in-app for user: {}", user.getId());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", inAppNotificationRequest);
            logger.info("Successfully published password change confirmation in-app for user: {}", user.getId());

        } catch (Exception e) {
            logger.error("Failed to publish password change confirmation notifications for user: {}. Error: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    private UserPasswordResponse buildUserPasswordResponse(int statusCode, boolean isSuccess, String message,
                                                           UserPasswordDto userPasswordDto, List<UserPasswordDto> userPasswordDtoList,
                                                           Page<UserPasswordDto> userPasswordDtoPage) {

        UserPasswordResponse userPasswordResponse = new UserPasswordResponse();
        userPasswordResponse.setStatusCode(statusCode);
        userPasswordResponse.setSuccess(isSuccess);
        userPasswordResponse.setMessage(message);
        userPasswordResponse.setUserPasswordDto(userPasswordDto);
        userPasswordResponse.setUserPasswordDtoList(userPasswordDtoList);
        userPasswordResponse.setUserPasswordDtoPage(userPasswordDtoPage);

        return userPasswordResponse;
    }

    private UserPasswordResponse buildUserPasswordResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                           UserPasswordDto userPasswordDto, List<UserPasswordDto> userPasswordDtoList,
                                                           List<String> errorMessages) {

        UserPasswordResponse userPasswordResponse = new UserPasswordResponse();
        userPasswordResponse.setStatusCode(statusCode);
        userPasswordResponse.setSuccess(isSuccess);
        userPasswordResponse.setMessage(message);
        userPasswordResponse.setUserPasswordDto(userPasswordDto);
        userPasswordResponse.setUserPasswordDtoList(userPasswordDtoList);
        userPasswordResponse.setErrorMessages(errorMessages);

        return userPasswordResponse;
    }
    
    /**
     * Find passwords that are about to expire within the specified date range
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of passwords that are about to expire with their associated users
     */
    @Override
    public ExpiringPasswordsResponse findPasswordsAboutToExpire(LocalDateTime startDate, LocalDateTime endDate) {

        logger.info("Finding passwords that will expire between {} and {}", startDate, endDate);

        Locale locale = Locale.getDefault();

        try {
            List<UserPassword> expiringPasswords = userPasswordRepository.findPasswordsAboutToExpire(
                    startDate, endDate, EntityStatus.DELETED);
            
            logger.info("Found {} passwords that will expire in the specified date range", expiringPasswords.size());
            
            List<ExpiringPasswordDto> expiringPasswordDtos = new ArrayList<>();
            
            for (UserPassword userPassword : expiringPasswords) {
                User user = userPassword.getUser();
                if (user != null) {
                    ExpiringPasswordDto dto = new ExpiringPasswordDto(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getPhoneNumber(),
                            userPassword.getId(),
                            userPassword.getExpiryDate(),
                            userPassword.getExpiryDate().isBefore(LocalDateTime.now())
                    );
                    expiringPasswordDtos.add(dto);
                }
            }
            
            ExpiringPasswordsResponse response = new ExpiringPasswordsResponse();
            response.setStatusCode(200);
            response.setSuccess(true);
            response.setMessage(
                    messageService.getMessage(I18Code.MESSAGE_EXPIRING_PASSWORDS_RETRIEVED_SUCCESSFULLY.getCode(),
                            new String[]{}, locale));
            response.setExpiringPasswords(expiringPasswordDtos);
            return response;
        } catch (Exception e) {
            logger.error("Error finding passwords that will expire: {}", e.getMessage(), e);
            ExpiringPasswordsResponse errorResponse = new ExpiringPasswordsResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setSuccess(false);
            errorResponse.setMessage(
                    messageService.getMessage(I18Code.MESSAGE_EXPIRING_PASSWORDS_RETRIEVE_FAILED.getCode(),
                            new String[]{}, locale));
            return errorResponse;
        }
    }
}
