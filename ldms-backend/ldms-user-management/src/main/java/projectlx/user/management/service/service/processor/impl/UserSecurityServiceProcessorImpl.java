package projectlx.user.management.service.service.processor.impl;

import projectlx.user.management.service.business.logic.api.UserSecurityService;
import projectlx.user.management.service.service.processor.api.UserSecurityServiceProcessor;
import projectlx.user.management.service.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.service.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.service.utils.requests.UserSecurityMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserSecurityResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@RequiredArgsConstructor
public class UserSecurityServiceProcessorImpl implements UserSecurityServiceProcessor {

    private final UserSecurityService userSecurityService;
    private final Logger logger = LoggerFactory.getLogger(UserSecurityServiceProcessorImpl.class);

    @Override
    public UserSecurityResponse create(CreateUserSecurityRequest createUserSecurityRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user security : {}", createUserSecurityRequest);

        UserSecurityResponse userSecurityResponse = userSecurityService.create(createUserSecurityRequest,
                locale, username);

        logger.info("Outgoing response after creating a user security : {}. Status Code: {}. Message: {}",
                userSecurityResponse, userSecurityResponse.getStatusCode(), userSecurityResponse.getMessage());

        return userSecurityResponse;
    }

    @Override
    public UserSecurityResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user security by id: {}", id);

        UserSecurityResponse userSecurityResponse = userSecurityService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user security by id : {}. Status Code: {}. Message: {}",
                userSecurityResponse, userSecurityResponse.getStatusCode(), userSecurityResponse.getMessage());

        return userSecurityResponse;
    }

    @Override
    public UserSecurityResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user securities as a list");

        UserSecurityResponse userSecurityResponse = userSecurityService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user securities as a list : {}. Status Code: {}. Message: {}",
                userSecurityResponse, userSecurityResponse.getStatusCode(), userSecurityResponse.getMessage());

        return userSecurityResponse;
    }

    @Override
    public UserSecurityResponse update(EditUserSecurityRequest editUserSecurityRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user security : {}", editUserSecurityRequest);

        UserSecurityResponse userSecurityResponse = userSecurityService.update(editUserSecurityRequest, username, locale);

        logger.info("Outgoing response after updating a user security : {}. Status Code: {}. Message: {}",
                userSecurityResponse, userSecurityResponse.getStatusCode(), userSecurityResponse.getMessage());

        return userSecurityResponse;
    }

    @Override
    public UserSecurityResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user security with the id : {}", id);

        UserSecurityResponse userSecurityResponse = userSecurityService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user security: {}. Status Code: {}. Message: {}", userSecurityResponse,
                userSecurityResponse.getStatusCode(), userSecurityResponse.getMessage());

        return userSecurityResponse;
    }

    @Override
    public UserSecurityResponse findByMultipleFilters(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest, String username, Locale locale) {

        logger.info("Incoming request to find a user security using multiple filters : {}", userSecurityMultipleFiltersRequest);

        UserSecurityResponse userSecurityResponse = userSecurityService.findByMultipleFilters(userSecurityMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user security using multiple filters: {}. Status Code: {}. Message: {}",
                userSecurityResponse, userSecurityResponse.getStatusCode(), userSecurityResponse.getMessage());

        return userSecurityResponse;
    }
}
