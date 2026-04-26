package projectlx.user.management.business.logic.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.business.logic.api.UserPreferencesService;
import projectlx.user.management.business.validator.api.UserPreferencesServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserPreferences;
import projectlx.user.management.repository.UserPreferencesRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.specification.UserPreferencesSpecification;
import projectlx.user.management.utils.dtos.UserPreferencesDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.utils.requests.UserPreferencesMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserPreferencesResponse;
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
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private final UserPreferencesServiceValidator userPreferencesServiceValidator;
    private final MessageService messageService;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final UserPreferencesServiceAuditable userPreferencesServiceAuditable;

    @Override
    public UserPreferencesResponse create(CreateUserPreferencesRequest createUserPreferencesRequest, Locale locale,
                                          String username) {

        String message = "";

        ValidatorDto validatorDto = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(
                createUserPreferencesRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserPreferencesResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(createUserPreferencesRequest.getUserId(),
                EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserPreferencesResponse(400, false, message, null,
                    null, null);
        }

        Optional<UserPreferences> userPreferencesRetrieved =
                userPreferencesRepository.findByIdAndEntityStatusNot(createUserPreferencesRequest.getUserId(),
                EntityStatus.DELETED);

        if (userPreferencesRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildUserPreferencesResponse(400, false, message, null,
                    null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserPreferences userPreferencesToBeSaved = modelMapper.map(createUserPreferencesRequest, UserPreferences.class);
        userPreferencesToBeSaved.setUser(userRetrieved.get());

        UserPreferences userPreferencesSaved = userPreferencesServiceAuditable.create(userPreferencesToBeSaved, locale,
                username);

        UserPreferencesDto userPreferencesDtoReturned = modelMapper.map(userPreferencesSaved, UserPreferencesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserPreferencesResponse(201, true, message, userPreferencesDtoReturned, null,
                null);
    }

    @Override
    public UserPreferencesResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userPreferencesServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserPreferencesResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserPreferences> userPreferencesRetrieved = userPreferencesRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userPreferencesRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserPreferencesResponse(404, false, message, null, null,
                    null);
        }

        UserPreferences userPreferencesReturned = userPreferencesRetrieved.get();
        UserPreferencesDto userPreferencesDto = modelMapper.map(userPreferencesReturned, UserPreferencesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserPreferencesResponse(200, true, message, userPreferencesDto, null,
                null);
    }

    @Override
    public UserPreferencesResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserPreferences> userPreferencesList = userPreferencesRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userPreferencesList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserPreferencesResponse(404, false, message, null,
                    null, null);
        }

        List<UserPreferencesDto> userPreferencesDtoList = modelMapper.map(userPreferencesList, new TypeToken<List<UserPreferencesDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserPreferencesResponse(200, true, message, null, userPreferencesDtoList,
                null);
    }

    @Override
    public UserPreferencesResponse update(EditUserPreferencesRequest editUserPreferencesRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserPreferencesResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserPreferences> userPreferencesRetrieved = userPreferencesRepository.findByIdAndEntityStatusNot(editUserPreferencesRequest.getId(), EntityStatus.DELETED);

        if (userPreferencesRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildUserPreferencesResponse(400, false, message, null,
                    null, null);
        }

        UserPreferences existingPreferences = userPreferencesRetrieved.get();

        // Check how many users share the same preferences
        long sharedCount = userPreferencesRepository.countByPreferredLanguageAndTimezoneAndEntityStatusNot(
                existingPreferences.getPreferredLanguage(),
                existingPreferences.getTimezone(),
                EntityStatus.DELETED
        );

        UserPreferences preferencesToPersist;

        if (sharedCount > 1) {

            // Multiple users share this preference, create a new record
            preferencesToPersist = new UserPreferences();
        } else {

            // Only this user uses this preference, update the existing record
            preferencesToPersist = existingPreferences;
        }

        // Apply updates
        applyUpdatesToUserPreferences(preferencesToPersist, editUserPreferencesRequest);

        // Save the new or updated preferences
        UserPreferences savedPreferences = userPreferencesServiceAuditable.update(preferencesToPersist, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserPreferencesDto preferencesDtoReturned = modelMapper.map(savedPreferences, UserPreferencesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserPreferencesResponse(201, true, message, preferencesDtoReturned,
                null, null);
    }

    @Override
    public UserPreferencesResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userPreferencesServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserPreferencesResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserPreferences> userPreferencesRetrieved = userPreferencesRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userPreferencesRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserPreferencesResponse(404, false, message, null, null,
                    null);
        }

        UserPreferences userPreferencesToBeDeleted = userPreferencesRetrieved.get();
        userPreferencesToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserPreferences userPreferencesDeleted = userPreferencesServiceAuditable.delete(userPreferencesToBeDeleted, locale);

        UserPreferencesDto useUserPreferencesDtoReturned = modelMapper.map(userPreferencesDeleted, UserPreferencesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserPreferencesResponse(200, true, message, useUserPreferencesDtoReturned, null,
                null);
    }

    @Override
    public UserPreferencesResponse findByMultipleFilters(UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest, String username, Locale locale) {
        String message = "";

        Specification<UserPreferences> spec = null;
        spec = addToSpec(spec, UserPreferencesSpecification::deleted);

        ValidatorDto validatorDto = userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(
                userPreferencesMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserPreferencesResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(userPreferencesMultipleFiltersRequest.getPage(),
                userPreferencesMultipleFiltersRequest.getSize());

        ValidatorDto preferredLanguageValidatorDto = userPreferencesServiceValidator.isStringValid(
                userPreferencesMultipleFiltersRequest.getPreferredLanguage(), locale);

        if (preferredLanguageValidatorDto.getSuccess()) {

            spec = addToSpec(userPreferencesMultipleFiltersRequest.getPreferredLanguage(), spec,
                    UserPreferencesSpecification::preferredLanguageLike);
        }

        ValidatorDto timeZoneValidatorDto = userPreferencesServiceValidator.isStringValid(
                userPreferencesMultipleFiltersRequest.getTimezone(), locale);

        if (timeZoneValidatorDto.getSuccess()) {

            spec = addToSpec(userPreferencesMultipleFiltersRequest.getPreferredLanguage(), spec,
                    UserPreferencesSpecification::timezoneLike);
        }

        // Check if searchValue exists before validating
        if (userPreferencesMultipleFiltersRequest.getSearchValue() != null) {
            ValidatorDto searchValueValidatorDto = userPreferencesServiceValidator.isStringValid(
                    userPreferencesMultipleFiltersRequest.getSearchValue(), locale);

            if (searchValueValidatorDto != null && searchValueValidatorDto.getSuccess()) {
                spec = addToSpec(userPreferencesMultipleFiltersRequest.getSearchValue(), spec, UserPreferencesSpecification::any);
            }
        }

        Page<UserPreferences> result = userPreferencesRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserPreferencesResponse(404, false, message,null, null,
                    null);
        }

        Page<UserPreferencesDto> UserPreferencesDtoPage = convertUserPreferencesEntityToUserPreferencesDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserPreferencesResponse(200, true, message,null, null,
                UserPreferencesDtoPage);
    }

    private Specification<UserPreferences> addToSpec(Specification<UserPreferences> spec,
                                               Function<EntityStatus, Specification<UserPreferences>> predicateMethod) {
        Specification<UserPreferences> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserPreferences> addToSpec(final String aString, Specification<UserPreferences> spec, Function<String,
            Specification<UserPreferences>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserPreferences> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<UserPreferencesDto> convertUserPreferencesEntityToUserPreferencesDto(Page<UserPreferences> UserPreferencesPage) {

        List<UserPreferences> UserPreferencesList = UserPreferencesPage.getContent();
        List<UserPreferencesDto> UserPreferencesDtoList = new ArrayList<>();

        for (UserPreferences UserPreferences : UserPreferencesPage) {
            UserPreferencesDto UserPreferencesDto = modelMapper.map(UserPreferences, UserPreferencesDto.class);
            UserPreferencesDtoList.add(UserPreferencesDto);
        }

        int page = UserPreferencesPage.getNumber();
        int size = UserPreferencesPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserPreferences = PageRequest.of(page, size);

        return new PageImpl<UserPreferencesDto>(UserPreferencesDtoList, pageableUserPreferences, UserPreferencesPage.getTotalElements());
    }

    private void applyUpdatesToUserPreferences(UserPreferences preferences, EditUserPreferencesRequest editRequest) {
        preferences.setPreferredLanguage(editRequest.getPreferredLanguage());
        preferences.setTimezone(editRequest.getTimezone());
    }

    private UserPreferencesResponse buildUserPreferencesResponse(int statusCode, boolean isSuccess, String message,
                                                                 UserPreferencesDto userPreferencesDto,
                                                                 List<UserPreferencesDto> userPreferencesDtoList,
                                                                 Page<UserPreferencesDto> userPreferencesDtoPage) {

        UserPreferencesResponse userPreferencesResponse = new UserPreferencesResponse();

        userPreferencesResponse.setStatusCode(statusCode);
        userPreferencesResponse.setSuccess(isSuccess);
        userPreferencesResponse.setMessage(message);
        userPreferencesResponse.setUserPreferencesDto(userPreferencesDto);
        userPreferencesResponse.setUserPreferencesDtoList(userPreferencesDtoList);
        userPreferencesResponse.setUserPreferencesDtoPage(userPreferencesDtoPage);

        return userPreferencesResponse;
    }

    private UserPreferencesResponse buildUserPreferencesResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                                 UserPreferencesDto userPreferencesDto,
                                                                 List<UserPreferencesDto> userPreferencesDtoList,
                                                                 List<String> errorMessages) {

        UserPreferencesResponse userPreferencesResponse = new UserPreferencesResponse();

        userPreferencesResponse.setStatusCode(statusCode);
        userPreferencesResponse.setSuccess(isSuccess);
        userPreferencesResponse.setMessage(message);
        userPreferencesResponse.setUserPreferencesDto(userPreferencesDto);
        userPreferencesResponse.setUserPreferencesDtoList(userPreferencesDtoList);
        userPreferencesResponse.setErrorMessages(errorMessages);

        return userPreferencesResponse;
    }
}
