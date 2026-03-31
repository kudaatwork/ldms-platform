package projectlx.user.management.service.service.processor.impl;

import projectlx.user.management.service.business.logic.api.UserPreferencesService;
import projectlx.user.management.service.service.processor.api.UserPreferencesServiceProcessor;
import projectlx.user.management.service.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.UserPreferencesMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserPreferencesResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@RequiredArgsConstructor
public class UserPreferencesServiceProcessorImpl implements UserPreferencesServiceProcessor {

    private final UserPreferencesService userPreferencesService;
    private final Logger logger = LoggerFactory.getLogger(UserAddressServiceProcessorImpl.class);

    @Override
    public UserPreferencesResponse create(CreateUserPreferencesRequest createUserPreferencesRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user preferences : {}", createUserPreferencesRequest);

        UserPreferencesResponse userPreferencesResponse = userPreferencesService.create(createUserPreferencesRequest,
                locale, username);

        logger.info("Outgoing response after creating a user preferences : {}. Status Code: {}. Message: {}",
                userPreferencesResponse, userPreferencesResponse.getStatusCode(), userPreferencesResponse.getMessage());

        return userPreferencesResponse;
    }

    @Override
    public UserPreferencesResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user preferences by id: {}", id);

        UserPreferencesResponse userPreferencesResponse = userPreferencesService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user preferences by id : {}. Status Code: {}. Message: {}",
                userPreferencesResponse, userPreferencesResponse.getStatusCode(), userPreferencesResponse.getMessage());

        return userPreferencesResponse;
    }

    @Override
    public UserPreferencesResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user preferences as a list");

        UserPreferencesResponse userPreferencesResponse = userPreferencesService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user preferences as a list : {}. Status Code: {}. Message: {}",
                userPreferencesResponse, userPreferencesResponse.getStatusCode(), userPreferencesResponse.getMessage());

        return userPreferencesResponse;
    }

    @Override
    public UserPreferencesResponse update(EditUserPreferencesRequest editUserPreferencesRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user preferences : {}", editUserPreferencesRequest);

        UserPreferencesResponse userPreferencesResponse = userPreferencesService.update(editUserPreferencesRequest, username, locale);

        logger.info("Outgoing response after updating a user preferences : {}. Status Code: {}. Message: {}",
                userPreferencesResponse, userPreferencesResponse.getStatusCode(), userPreferencesResponse.getMessage());

        return userPreferencesResponse;
    }

    @Override
    public UserPreferencesResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user preferences with the id : {}", id);

        UserPreferencesResponse userPreferencesResponse = userPreferencesService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user preferences: {}. Status Code: {}. Message: {}", userPreferencesResponse,
                userPreferencesResponse.getStatusCode(), userPreferencesResponse.getMessage());

        return userPreferencesResponse;
    }

    @Override
    public UserPreferencesResponse findByMultipleFilters(UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest, String username, Locale locale) {

        logger.info("Incoming request to find a user preferences using multiple filters : {}", userPreferencesMultipleFiltersRequest);

        UserPreferencesResponse userPreferencesResponse = userPreferencesService.findByMultipleFilters(userPreferencesMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user preferences using multiple filters: {}. Status Code: {}. Message: {}",
                userPreferencesResponse, userPreferencesResponse.getStatusCode(), userPreferencesResponse.getMessage());

        return userPreferencesResponse;
    }
}
