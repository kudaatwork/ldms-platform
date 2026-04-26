package projectlx.user.management.business.logic.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.business.logic.api.UserSecurityService;
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
import projectlx.user.management.utils.requests.UserSecurityMultipleFiltersRequest;
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

    private void applyUpdatesToUserSecurity(UserSecurity userSecurity, EditUserSecurityRequest editRequest) {
        userSecurity.setSecurityQuestion_1(editRequest.getSecurityQuestion_1());
        userSecurity.setSecurityAnswer_1(editRequest.getSecurityAnswer_1());
        userSecurity.setSecurityQuestion_2(editRequest.getSecurityQuestion_2());
        userSecurity.setSecurityAnswer_2(editRequest.getSecurityAnswer_2());
        userSecurity.setTwoFactorAuthSecret(editRequest.getTwoFactorAuthSecret());
        userSecurity.setIsTwoFactorEnabled(editRequest.getIsTwoFactorEnabled());
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
