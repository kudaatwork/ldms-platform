package projectlx.user.management.business.logic.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.business.logic.api.UserSecurityService;
import projectlx.user.management.business.logic.support.TwoFactorSelfServiceSupport;
import projectlx.user.management.business.logic.support.TwoFactorSelfServiceSupport.TwoFactorSelfServiceException;
import projectlx.user.management.business.validator.api.UserSecurityServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserSecurity;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.repository.specification.UserSecuritySpecification;
import projectlx.user.management.utils.dtos.UserSecurityDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.utils.requests.TwoFactorOtpRequest;
import projectlx.user.management.utils.requests.UserSecurityMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.security.TotpSupport;
import org.springframework.util.StringUtils;
import projectlx.user.management.utils.responses.UserSecurityResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class UserSecurityServiceImpl implements UserSecurityService {

    private final UserSecurityServiceValidator userSecurityServiceValidator;
    private final MessageService messageService;
    private final UserSecurityRepository userSecurityRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final UserSecurityServiceAuditable userSecurityServiceAuditable;
    private final TwoFactorSelfServiceSupport twoFactorSelfServiceSupport;

    @Override
    public UserSecurityResponse create(CreateUserSecurityRequest createUserSecurityRequest, Locale locale,
                                       String username) {

        String message = "";

        ValidatorDto validatorDto = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_SECURITY_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserSecurityResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(createUserSecurityRequest.getUserId(),
                EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserSecurityResponse(400, false, message, null,
                    null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserSecurity userSecurityToBeSaved = modelMapper.map(createUserSecurityRequest, UserSecurity.class);
        userSecurityToBeSaved.setUser(userRetrieved.get());
        UserSecurity userSecuritySaved = userSecurityServiceAuditable.create(userSecurityToBeSaved, locale,
                username);

        UserSecurityDto userSecurityDtoReturned = modelMapper.map(userSecuritySaved, UserSecurityDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserSecurityResponse(201, true, message, userSecurityDtoReturned, null,
                null);
    }

    @Override
    public UserSecurityResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userSecurityServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserSecurityResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserSecurity> userSecurityRetrieved = userSecurityRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userSecurityRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserSecurityResponse(404, false, message, null, null,
                    null);
        }

        UserSecurity userSecurityReturned = userSecurityRetrieved.get();
        UserSecurityDto userSecurityDto = modelMapper.map(userSecurityReturned, UserSecurityDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserSecurityResponse(200, true, message, userSecurityDto, null,
                null);
    }

    @Override
    public UserSecurityResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserSecurity> userSecurityList = userSecurityRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userSecurityList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserSecurityResponse(404, false, message, null,
                    null, null);
        }

        List<UserSecurityDto> userSecurityDtoList = modelMapper.map(userSecurityList, new TypeToken<List<UserSecurityDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserSecurityResponse(200, true, message, null, userSecurityDtoList,
                null);
    }

    @Override
    public UserSecurityResponse update(EditUserSecurityRequest editUserSecurityRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_SECURITY_INVALID_REQUEST.getCode(), new String[]{}, locale);

            return buildUserSecurityResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserSecurity> userSecurityRetrieved = userSecurityRepository.findByIdAndEntityStatusNot(
                editUserSecurityRequest.getId(), EntityStatus.DELETED);

        if (userSecurityRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildUserSecurityResponse(400, false, message, null,
                    null, null);
        }

        UserSecurity existingSecurity = userSecurityRetrieved.get();

        // Count how many users have the same security setup
        long sharedCount = userSecurityRepository.countMatchingSecuritySetup(existingSecurity.getSecurityQuestion_1(),
                existingSecurity.getSecurityAnswer_1(), existingSecurity.getSecurityQuestion_2(),
                existingSecurity.getSecurityAnswer_2(), EntityStatus.DELETED
        );

        UserSecurity securityToPersist;

        if (sharedCount > 1) {

            // Shared security setup detected - create a new record
            securityToPersist = new UserSecurity();

        } else {

            // Unique security setup - update existing
            securityToPersist = existingSecurity;
        }

        // Apply changes from the request
        applyUpdatesToUserSecurity(securityToPersist, editUserSecurityRequest);

        // Save updated or new security record
        UserSecurity savedSecurity = userSecurityServiceAuditable.update(securityToPersist, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserSecurityDto userSecurityDtoReturned = modelMapper.map(savedSecurity, UserSecurityDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserSecurityResponse(201, true, message, userSecurityDtoReturned,
                null, null);
    }

    @Override
    public UserSecurityResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userSecurityServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserSecurityResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserSecurity> userSecurityRetrieved = userSecurityRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userSecurityRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserSecurityResponse(404, false, message, null, null,
                    null);
        }

        UserSecurity userSecurityToBeDeleted = userSecurityRetrieved.get();
        userSecurityToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserSecurity userSecurityDeleted = userSecurityServiceAuditable.delete(userSecurityToBeDeleted, locale);

        UserSecurityDto useSecurityDtoReturned = modelMapper.map(userSecurityDeleted, UserSecurityDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserSecurityResponse(200, true, message, useSecurityDtoReturned, null,
                null);
    }

    @Override
    public UserSecurityResponse findByMultipleFilters(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest, String username, Locale locale) {

        String message = "";

        Specification<UserSecurity> spec = null;
        spec = addToSpec(spec, UserSecuritySpecification::deleted);

        ValidatorDto validatorDto = userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(
                userSecurityMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserSecurityResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(userSecurityMultipleFiltersRequest.getPage(),
                userSecurityMultipleFiltersRequest.getSize());

        ValidatorDto twoFactorEnabledValidatorDto = userSecurityServiceValidator.isBooleanValid(
                userSecurityMultipleFiltersRequest.getIsTwoFactorEnabled(), locale);

        if (twoFactorEnabledValidatorDto.getSuccess()) {

            spec = addToSpec(userSecurityMultipleFiltersRequest.getIsTwoFactorEnabled(), spec,
                    UserSecuritySpecification::isTwoFactorEnabledLike);
        }

        ValidatorDto searchValueValidatorDto = userSecurityServiceValidator.isStringValid(
                userSecurityMultipleFiltersRequest.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {

            spec = addToSpec(userSecurityMultipleFiltersRequest.getSearchValue(), spec, UserSecuritySpecification::any);
        }

        Page<UserSecurity> result = userSecurityRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserSecurityResponse(404, false, message,null, null,
                    null);
        }

        Page<UserSecurityDto> userSecurityDtoPage = convertUserSecurityEntityToUserSecurityDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserSecurityResponse(200, true, message,null, null,
                userSecurityDtoPage);
    }

    private Specification<UserSecurity> addToSpec(Specification<UserSecurity> spec,
                                               Function<EntityStatus, Specification<UserSecurity>> predicateMethod) {
        Specification<UserSecurity> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserSecurity> addToSpec(final String aString, Specification<UserSecurity> spec, Function<String,
            Specification<UserSecurity>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserSecurity> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<UserSecurity> addToSpec(final Boolean aBoolean, Specification<UserSecurity> spec, Function<Boolean,
            Specification<UserSecurity>> predicateMethod) {
        if (aBoolean != null) {
            Specification<UserSecurity> localSpec = Specification.where(predicateMethod.apply(aBoolean));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<UserSecurityDto> convertUserSecurityEntityToUserSecurityDto(Page<UserSecurity> userSecurityPage) {

        List<UserSecurity> userSecurityList = userSecurityPage.getContent();
        List<UserSecurityDto> userSecurityDtoList = new ArrayList<>();

        for (UserSecurity userSecurity : userSecurityPage) {
            UserSecurityDto userSecurityDto = modelMapper.map(userSecurity, UserSecurityDto.class);
            userSecurityDtoList.add(userSecurityDto);
        }

        int page = userSecurityPage.getNumber();
        int size = userSecurityPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserSecurities = PageRequest.of(page, size);

        return new PageImpl<UserSecurityDto>(userSecurityDtoList, pageableUserSecurities, userSecurityPage.getTotalElements());
    }

    @Override
    public UserSecurityResponse findMySecurity(Locale locale, String sessionUsername) {
        Optional<User> userOpt = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                sessionUsername, EntityStatus.DELETED);
        if (userOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[] {}, locale);
            return buildUserSecurityResponse(404, false, message, null, null, null);
        }
        Optional<UserSecurity> securityOpt = userSecurityRepository.findByUser_IdAndEntityStatusNot(
                userOpt.get().getId(), EntityStatus.DELETED);
        if (securityOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode(), new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message, null, null, null);
        }
        UserSecurityDto dto = twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(securityOpt.get());
        String message = messageService.getMessage(
                I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode(), new String[] {}, locale);
        return buildUserSecurityResponse(200, true, message, dto, null, null);
    }

    @Override
    public UserSecurityResponse saveMySecurity(EditUserSecurityRequest request, Locale locale, String sessionUsername) {
        Optional<User> userOpt = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                sessionUsername, EntityStatus.DELETED);
        if (userOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[] {}, locale);
            return buildUserSecurityResponse(404, false, message, null, null, null);
        }
        Long userId = userOpt.get().getId();
        request.setUserId(userId);

        Optional<UserSecurity> existing = userSecurityRepository.findByUser_IdAndEntityStatusNot(userId, EntityStatus.DELETED);
        if (existing.isPresent()) {
            UserSecurity row = existing.get();
            request.setId(row.getId());
            if (!StringUtils.hasText(request.getTwoFactorAuthSecret())) {
                request.setTwoFactorAuthSecret(row.getTwoFactorAuthSecret());
            }
            if (request.getIsTwoFactorEnabled() == null) {
                request.setIsTwoFactorEnabled(row.getIsTwoFactorEnabled());
            }
            if (request.getTwoFactorMethod() == null) {
                request.setTwoFactorMethod(row.getTwoFactorMethod());
            }
            return update(request, sessionUsername, locale);
        }

        if (!StringUtils.hasText(request.getTwoFactorAuthSecret())) {
            request.setTwoFactorAuthSecret(TotpSupport.generateSecret());
        }
        if (request.getIsTwoFactorEnabled() == null) {
            request.setIsTwoFactorEnabled(Boolean.FALSE);
        }

        CreateUserSecurityRequest createRequest = new CreateUserSecurityRequest();
        createRequest.setUserId(userId);
        createRequest.setSecurityQuestion_1(request.getSecurityQuestion_1());
        createRequest.setSecurityAnswer_1(request.getSecurityAnswer_1());
        createRequest.setSecurityQuestion_2(request.getSecurityQuestion_2());
        createRequest.setSecurityAnswer_2(request.getSecurityAnswer_2());
        createRequest.setTwoFactorAuthSecret(request.getTwoFactorAuthSecret());
        createRequest.setIsTwoFactorEnabled(
                request.getIsTwoFactorEnabled() != null ? request.getIsTwoFactorEnabled() : Boolean.FALSE);
        return create(createRequest, locale, sessionUsername);
    }

    @Override
    public UserSecurityResponse beginMyAuthenticatorSetup(Locale locale, String sessionUsername) {
        return runTwoFactorSelfService(locale, sessionUsername, user -> {
            TwoFactorSelfServiceSupport.BeginAuthenticatorSetup setup =
                    twoFactorSelfServiceSupport.beginAuthenticatorSetup(user, sessionUsername);
            UserSecurityResponse response = buildUserSecurityResponse(
                    200,
                    true,
                    messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_AUTHENTICATOR_SETUP_STARTED.getCode(),
                            new String[] {}, locale),
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(
                            twoFactorSelfServiceSupport.findActiveSecurity(user).orElseThrow()),
                    null,
                    null);
            response.setAuthenticatorSetupSecret(setup.secret());
            response.setAuthenticatorSetupOtpAuthUri(setup.otpAuthUri());
            response.setAuthenticatorSetupQrCodeDataUrl(setup.qrCodeDataUrl());
            return response;
        });
    }

    @Override
    public UserSecurityResponse confirmMyAuthenticatorSetup(TwoFactorOtpRequest request, Locale locale,
                                                            String sessionUsername) {
        return runTwoFactorSelfService(locale, sessionUsername, user -> {
            UserSecurity saved = twoFactorSelfServiceSupport.confirmAuthenticatorSetup(user, request, sessionUsername);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_AUTHENTICATOR_ENABLED.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message,
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(saved), null, null);
        });
    }

    @Override
    public UserSecurityResponse enableMySmsTwoFactor(Locale locale, String sessionUsername) {
        return runTwoFactorSelfService(locale, sessionUsername, user -> {
            UserSecurity saved = twoFactorSelfServiceSupport.enableSmsTwoFactor(user, sessionUsername);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_SMS_ENABLED.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message,
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(saved), null, null);
        });
    }

    @Override
    public UserSecurityResponse requestMyTwoFactorDisableOtp(Locale locale, String sessionUsername) {
        return runTwoFactorSelfService(locale, sessionUsername, user -> {
            twoFactorSelfServiceSupport.sendSmsVerificationForDisable(user, sessionUsername);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_DISABLE_OTP_SENT.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message, null, null, null);
        });
    }

    @Override
    public UserSecurityResponse disableMyTwoFactor(TwoFactorOtpRequest request, Locale locale, String sessionUsername) {
        return runTwoFactorSelfService(locale, sessionUsername, user -> {
            UserSecurity saved = twoFactorSelfServiceSupport.disableTwoFactor(user, request, sessionUsername);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_DISABLED.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message,
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(saved), null, null);
        });
    }

    @Override
    public UserSecurityResponse adminBeginAuthenticatorSetup(Long userId, Locale locale, String actor) {
        return runAdminTwoFactor(userId, locale, user -> {
            TwoFactorSelfServiceSupport.BeginAuthenticatorSetup setup =
                    twoFactorSelfServiceSupport.beginAuthenticatorSetup(user, actor);
            UserSecurityResponse response = buildUserSecurityResponse(
                    200,
                    true,
                    messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_AUTHENTICATOR_SETUP_STARTED.getCode(),
                            new String[] {}, locale),
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(
                            twoFactorSelfServiceSupport.findActiveSecurity(user).orElseThrow()),
                    null,
                    null);
            response.setAuthenticatorSetupSecret(setup.secret());
            response.setAuthenticatorSetupOtpAuthUri(setup.otpAuthUri());
            response.setAuthenticatorSetupQrCodeDataUrl(setup.qrCodeDataUrl());
            return response;
        });
    }

    @Override
    public UserSecurityResponse adminConfirmAuthenticatorSetup(Long userId, TwoFactorOtpRequest request,
                                                               Locale locale, String actor) {
        return runAdminTwoFactor(userId, locale, user -> {
            UserSecurity saved = twoFactorSelfServiceSupport.confirmAuthenticatorSetup(user, request, actor);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_AUTHENTICATOR_ENABLED.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message,
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(saved), null, null);
        });
    }

    @Override
    public UserSecurityResponse adminEnableSmsTwoFactor(Long userId, Locale locale, String actor) {
        return runAdminTwoFactor(userId, locale, user -> {
            UserSecurity saved = twoFactorSelfServiceSupport.enableSmsTwoFactor(user, actor);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_SMS_ENABLED.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message,
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(saved), null, null);
        });
    }

    @Override
    public UserSecurityResponse adminDisableTwoFactor(Long userId, Locale locale, String actor) {
        return runAdminTwoFactor(userId, locale, user -> {
            UserSecurity saved = twoFactorSelfServiceSupport.disableTwoFactorAsAdmin(user, actor);
            String message = messageService.getMessage(I18Code.MESSAGE_TWO_FACTOR_DISABLED.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(200, true, message,
                    twoFactorSelfServiceSupport.sanitizeForSelfServiceRead(saved), null, null);
        });
    }

    private UserSecurityResponse runAdminTwoFactor(Long userId, Locale locale,
                                                   java.util.function.Function<User, UserSecurityResponse> action) {
        if (userId == null || userId < 1) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(),
                    new String[] {}, locale);
            return buildUserSecurityResponse(400, false, message, null, null, null);
        }
        Optional<User> userOpt = userRepository.findByIdAndEntityStatusNot(userId, EntityStatus.DELETED);
        if (userOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[] {}, locale);
            return buildUserSecurityResponse(404, false, message, null, null, null);
        }
        try {
            return action.apply(userOpt.get());
        } catch (TwoFactorSelfServiceException ex) {
            return buildTwoFactorErrorResponse(ex, locale);
        }
    }

    private UserSecurityResponse runTwoFactorSelfService(Locale locale, String sessionUsername,
                                                         java.util.function.Function<User, UserSecurityResponse> action) {
        Optional<User> userOpt = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                sessionUsername, EntityStatus.DELETED);
        if (userOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[] {}, locale);
            return buildUserSecurityResponse(404, false, message, null, null, null);
        }
        try {
            return action.apply(userOpt.get());
        } catch (TwoFactorSelfServiceException ex) {
            return buildTwoFactorErrorResponse(ex, locale);
        }
    }

    private UserSecurityResponse buildTwoFactorErrorResponse(TwoFactorSelfServiceException ex, Locale locale) {
        I18Code code = switch (ex.getError()) {
            case ALREADY_ENABLED -> I18Code.MESSAGE_TWO_FACTOR_ALREADY_ENABLED;
            case NOT_ENABLED -> I18Code.MESSAGE_TWO_FACTOR_NOT_ENABLED;
            case SETUP_NOT_STARTED -> I18Code.MESSAGE_TWO_FACTOR_SETUP_NOT_STARTED;
            case OTP_INVALID -> I18Code.MESSAGE_OTP_INVALID_OR_EXPIRED;
            case PHONE_MISSING -> I18Code.MESSAGE_PHONE_NUMBER_MISSING_FOR_VERIFICATION;
            case PHONE_NOT_VERIFIED -> I18Code.MESSAGE_TWO_FACTOR_PHONE_NOT_VERIFIED;
            case SECURITY_NOT_FOUND -> I18Code.MESSAGE_USER_SECURITY_NOT_FOUND;
            case WRONG_METHOD -> I18Code.MESSAGE_TWO_FACTOR_WRONG_METHOD;
        };
        String message = messageService.getMessage(code.getCode(), new String[] {}, locale);
        return buildUserSecurityResponse(400, false, message, null, null, null);
    }

    private void applyUpdatesToUserSecurity(UserSecurity userSecurity, EditUserSecurityRequest editRequest) {
        userSecurity.setSecurityQuestion_1(editRequest.getSecurityQuestion_1());
        userSecurity.setSecurityAnswer_1(editRequest.getSecurityAnswer_1());
        userSecurity.setSecurityQuestion_2(editRequest.getSecurityQuestion_2());
        userSecurity.setSecurityAnswer_2(editRequest.getSecurityAnswer_2());
        userSecurity.setTwoFactorAuthSecret(editRequest.getTwoFactorAuthSecret());
        userSecurity.setIsTwoFactorEnabled(editRequest.getIsTwoFactorEnabled());
        if (editRequest.getTwoFactorMethod() != null) {
            userSecurity.setTwoFactorMethod(editRequest.getTwoFactorMethod());
        }
    }

    private UserSecurityResponse buildUserSecurityResponse(int statusCode, boolean isSuccess, String message,
                                                           UserSecurityDto userSecurityDto, List<UserSecurityDto> userSecurityDtoList,
                                                           Page<UserSecurityDto> userSecurityDtoPage) {

        UserSecurityResponse userSecurityResponse = new UserSecurityResponse();

        userSecurityResponse.setStatusCode(statusCode);
        userSecurityResponse.setSuccess(isSuccess);
        userSecurityResponse.setMessage(message);
        userSecurityResponse.setUserSecurityDto(userSecurityDto);
        userSecurityResponse.setUserSecurityDtoList(userSecurityDtoList);
        userSecurityResponse.setUserSecurityDtoPage(userSecurityDtoPage);

        return userSecurityResponse;
    }

    private UserSecurityResponse buildUserSecurityResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                     UserSecurityDto userSecurityDto, List<UserSecurityDto> userSecurityDtoList,
                                                     List<String> errorMessages) {

        UserSecurityResponse userSecurityResponse = new UserSecurityResponse();

        userSecurityResponse.setStatusCode(statusCode);
        userSecurityResponse.setSuccess(isSuccess);
        userSecurityResponse.setMessage(message);
        userSecurityResponse.setUserSecurityDto(userSecurityDto);
        userSecurityResponse.setUserSecurityDtoList(userSecurityDtoList);
        userSecurityResponse.setErrorMessages(errorMessages);

        return userSecurityResponse;
    }
}
