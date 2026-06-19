package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.LogisticsRouteStopServiceAuditable;
import projectlx.inventory.management.business.logic.api.LogisticsRouteStopService;
import projectlx.inventory.management.business.validator.api.LogisticsRouteStopServiceValidator;
import projectlx.inventory.management.model.LogisticsRouteStop;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.repository.LogisticsRouteStopRepository;
import projectlx.inventory.management.utils.dtos.LogisticsRouteStopDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.inventory.management.utils.requests.RouteStopRequest;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class LogisticsRouteStopServiceImpl implements LogisticsRouteStopService {

    private final LogisticsRouteStopRepository repository;
    private final LogisticsRouteStopServiceAuditable auditable;
    private final LogisticsRouteStopServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    // ============================================================
    // REPLACE ROUTE STOPS (primary operation)
    // ============================================================

    @Override
    @Transactional
    public LogisticsRouteStopResponse replaceRouteStops(
            RouteStopContextType contextType,
            Long contextId,
            List<RouteStopRequest> stops,
            Long organizationId,
            Locale locale,
            String username) {

        ReplaceRouteStopsRequest request = new ReplaceRouteStopsRequest();
        request.setContextType(contextType);
        request.setContextId(contextId);
        request.setOrganizationId(organizationId);
        request.setStops(stops != null ? stops : new ArrayList<>());
        return replaceRouteStops(request, locale, username);
    }

    @Override
    @Transactional
    public LogisticsRouteStopResponse replaceRouteStops(
            ReplaceRouteStopsRequest request, Locale locale, String username) {

        String message;

        // ============================================================
        // STEP 1: Validate incoming request
        // ============================================================
        ValidatorDto validatorDto = validator.isReplaceRouteStopsRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_ROUTE_STOP_INVALID_REQUEST.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Soft-delete all existing stops for this context
        // ============================================================
        int deleted = repository.softDeleteByContext(
                request.getContextType(), request.getContextId(), EntityStatus.DELETED);
        log.info("Soft-deleted {} route stops for context [{}/{}]",
                deleted, request.getContextType(), request.getContextId());

        // ============================================================
        // STEP 3: Insert new stops in order
        // ============================================================
        List<LogisticsRouteStopDto> savedDtos = new ArrayList<>();
        if (request.getStops() != null && !request.getStops().isEmpty()) {
            int sequence = 1;
            for (RouteStopRequest stopReq : request.getStops()) {
                LogisticsRouteStop stop = new LogisticsRouteStop();
                stop.setOrganizationId(request.getOrganizationId());
                stop.setContextType(request.getContextType());
                stop.setContextId(request.getContextId());
                stop.setStopSequence(stopReq.getStopSequence() != null ? stopReq.getStopSequence() : sequence);
                stop.setStopType(stopReq.getStopType());
                stop.setWarehouseLocationId(stopReq.getWarehouseLocationId());
                stop.setBranchId(stopReq.getBranchId());
                stop.setLocationLabel(stopReq.getLocationLabel());
                stop.setEntityStatus(EntityStatus.ACTIVE);
                stop.setCreatedBy(username);

                LogisticsRouteStop saved = auditable.create(stop, locale, username);
                savedDtos.add(mapToDto(saved));
                sequence++;
            }
        }

        message = messageService.getMessage(
                I18Code.MESSAGE_ROUTE_STOP_REPLACED_SUCCESSFULLY.getCode(), locale);
        log.info("Replaced {} route stops for context [{}/{}] by user [{}]",
                savedDtos.size(), request.getContextType(), request.getContextId(), username);

        return buildResponse(200, true, message, null, savedDtos, null);
    }

    // ============================================================
    // QUERY OPERATIONS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public LogisticsRouteStopResponse findByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username) {

        List<LogisticsRouteStop> stops = repository
                .findByContextTypeAndContextIdAndEntityStatusNotOrderByStopSequenceAsc(
                        contextType, contextId, EntityStatus.DELETED);

        List<LogisticsRouteStopDto> dtoList = stops.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        String message = messageService.getMessage(
                I18Code.MESSAGE_ROUTE_STOP_RETRIEVED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LogisticsRouteStopResponse findById(Long id, Locale locale, String username) {

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<LogisticsRouteStop> optional = repository.findByIdAndEntityStatusNot(
                id, EntityStatus.DELETED);

        if (optional.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_ROUTE_STOP_NOT_FOUND.getCode(), locale);
            return buildResponse(404, false, message, null, null, null);
        }

        String message = messageService.getMessage(
                I18Code.MESSAGE_ROUTE_STOP_RETRIEVED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, mapToDto(optional.get()), null, null);
    }

    @Override
    @Transactional
    public LogisticsRouteStopResponse deleteByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username) {

        int deleted = repository.softDeleteByContext(contextType, contextId, EntityStatus.DELETED);
        log.info("Soft-deleted {} route stops for context [{}/{}] by user [{}]",
                deleted, contextType, contextId, username);

        String message = messageService.getMessage(
                I18Code.MESSAGE_ROUTE_STOP_DELETED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, null, null, null);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private LogisticsRouteStopDto mapToDto(LogisticsRouteStop stop) {
        return modelMapper.map(stop, LogisticsRouteStopDto.class);
    }

    private LogisticsRouteStopResponse buildResponse(
            int statusCode, boolean success, String message,
            LogisticsRouteStopDto dto,
            List<LogisticsRouteStopDto> dtoList,
            List<String> errors) {

        LogisticsRouteStopResponse response = new LogisticsRouteStopResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setLogisticsRouteStopDto(dto);
        response.setLogisticsRouteStopDtoList(dtoList);
        if (errors != null && !errors.isEmpty()) {
            response.setErrorMessages(errors);
        }
        return response;
    }
}
