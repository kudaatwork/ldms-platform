package projectlx.co.zw.fleetmanagement.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetDriverService;
import projectlx.co.zw.fleetmanagement.business.logic.support.CallerOrganizationResolver;
import projectlx.co.zw.fleetmanagement.business.logic.support.FleetMapper;
import projectlx.co.zw.fleetmanagement.business.validation.api.FleetDriverServiceValidator;
import projectlx.co.zw.fleetmanagement.model.FleetDriver;
import projectlx.co.zw.fleetmanagement.repository.FleetDriverRepository;
import projectlx.co.zw.fleetmanagement.utils.enums.I18Code;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetDriverResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FleetDriverServiceImpl implements FleetDriverService {

    private final FleetDriverServiceValidator fleetDriverServiceValidator;
    private final FleetDriverRepository fleetDriverRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        List<FleetDriver> drivers = fleetDriverRepository.findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
                organizationId, EntityStatus.DELETED);
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDtoList(drivers.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username) {
        ValidatorDto validation = fleetDriverServiceValidator.isCreateFleetDriverRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetDriver driver = new FleetDriver();
        driver.setOrganizationId(organizationId);
        mapRequestToEntity(request, driver);
        driver.setEntityStatus(EntityStatus.ACTIVE);
        driver.setCreatedAt(LocalDateTime.now());
        driver.setCreatedBy(username);

        FleetDriver saved = fleetDriverRepository.save(driver);
        FleetDriverResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username) {
        if (request != null) {
            request.setId(id);
        }
        ValidatorDto validation = fleetDriverServiceValidator.isEditFleetDriverRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetDriver driver = fleetDriverRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        mapEditRequestToEntity(request, driver);
        driver.setModifiedAt(LocalDateTime.now());
        driver.setModifiedBy(username);

        FleetDriver saved = fleetDriverRepository.save(driver);
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse delete(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetDriver driver = fleetDriverRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        driver.setEntityStatus(EntityStatus.DELETED);
        driver.setModifiedAt(LocalDateTime.now());
        driver.setModifiedBy(username);
        fleetDriverRepository.save(driver);

        return successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_DELETE_SUCCESS.getCode(), new String[]{}, locale));
    }

    private void mapRequestToEntity(CreateFleetDriverRequest request, FleetDriver driver) {
        applyDriverFields(request.getUserId(), request.getFirstName(), request.getLastName(),
                request.getPhoneNumber(), request.getLicenseNumber(), request.getLicenseClass(), driver);
    }

    private void mapEditRequestToEntity(EditFleetDriverRequest request, FleetDriver driver) {
        applyDriverFields(request.getUserId(), request.getFirstName(), request.getLastName(),
                request.getPhoneNumber(), request.getLicenseNumber(), request.getLicenseClass(), driver);
    }

    private void applyDriverFields(Long userId, String firstName, String lastName, String phoneNumber,
                                   String licenseNumber, String licenseClass, FleetDriver driver) {
        driver.setUserId(userId);
        driver.setFirstName(firstName.trim());
        driver.setLastName(lastName.trim());
        driver.setPhoneNumber(phoneNumber);
        driver.setLicenseNumber(licenseNumber.trim());
        driver.setLicenseClass(licenseClass);
    }

    private FleetDriverResponse successResponse(int statusCode, String message) {
        FleetDriverResponse response = new FleetDriverResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private FleetDriverResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private FleetDriverResponse errorResponse(int statusCode, String message, List<String> errors) {
        FleetDriverResponse response = new FleetDriverResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
