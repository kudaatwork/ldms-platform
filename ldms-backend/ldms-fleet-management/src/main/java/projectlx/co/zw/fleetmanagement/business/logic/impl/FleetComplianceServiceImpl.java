package projectlx.co.zw.fleetmanagement.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetComplianceService;
import projectlx.co.zw.fleetmanagement.business.logic.support.CallerOrganizationResolver;
import projectlx.co.zw.fleetmanagement.business.logic.support.ComplianceStatusResolver;
import projectlx.co.zw.fleetmanagement.business.logic.support.FleetFileUploadHelper;
import projectlx.co.zw.fleetmanagement.business.logic.support.FleetMapper;
import projectlx.co.zw.fleetmanagement.business.validation.api.FleetComplianceServiceValidator;
import projectlx.co.zw.fleetmanagement.model.FleetComplianceRecord;
import projectlx.co.zw.fleetmanagement.repository.FleetAssetRepository;
import projectlx.co.zw.fleetmanagement.repository.FleetComplianceRecordRepository;
import projectlx.co.zw.fleetmanagement.repository.FleetDriverRepository;
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceStatus;
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceSubjectType;
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceType;
import projectlx.co.zw.fleetmanagement.utils.enums.I18Code;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetComplianceRecordResponse;
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
public class FleetComplianceServiceImpl implements FleetComplianceService {

    private final FleetComplianceServiceValidator fleetComplianceServiceValidator;
    private final FleetComplianceRecordRepository fleetComplianceRecordRepository;
    private final FleetAssetRepository fleetAssetRepository;
    private final FleetDriverRepository fleetDriverRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final FleetFileUploadHelper fleetFileUploadHelper;
    private final MessageService messageService;

    @Value("${ldms.fleet.compliance-expiring-soon-days:30}")
    private int defaultExpiringSoonDays;

    @Override
    @Transactional(readOnly = true)
    public FleetComplianceRecordResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        List<FleetComplianceRecord> records = fleetComplianceRecordRepository
                .findByOrganizationIdAndEntityStatusNotOrderByIdDesc(organizationId, EntityStatus.DELETED);
        FleetComplianceRecordResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetComplianceRecordDtoList(records.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetComplianceRecordResponse create(CreateFleetComplianceRecordRequest request, Locale locale, String username) {
        ValidatorDto validation = fleetComplianceServiceValidator.isCreateFleetComplianceRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        ComplianceSubjectType subjectType = ComplianceSubjectType.valueOf(request.getSubjectType().trim().toUpperCase());
        if (!subjectExists(organizationId, subjectType, request.getSubjectId())) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_SUBJECT_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        if (!fleetFileUploadHelper.validateFileUploadReference(
                request.getFileUploadId(), subjectType, request.getSubjectId(), locale)) {
            return errorResponse(400, fleetFileUploadHelper.fileUploadInvalidMessage(locale));
        }

        LocalDateTime expiresAt = fleetFileUploadHelper.resolveExpiresAt(request.getExpiresAt(), request.getFileUploadId());

        FleetComplianceRecord record = new FleetComplianceRecord();
        record.setOrganizationId(organizationId);
        record.setSubjectType(subjectType);
        record.setSubjectId(request.getSubjectId());
        record.setComplianceType(ComplianceType.valueOf(request.getComplianceType().trim().toUpperCase()));
        record.setFileUploadId(request.getFileUploadId());
        record.setExpiresAt(expiresAt);
        record.setStatus(ComplianceStatusResolver.resolve(expiresAt, request.getFileUploadId(), defaultExpiringSoonDays));
        record.setNotes(request.getNotes());
        record.setEntityStatus(EntityStatus.ACTIVE);
        record.setCreatedAt(LocalDateTime.now());
        record.setCreatedBy(username);

        FleetComplianceRecord saved = fleetComplianceRecordRepository.save(record);
        FleetComplianceRecordResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetComplianceRecordDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetComplianceRecordResponse update(Long id, EditFleetComplianceRecordRequest request, Locale locale, String username) {
        if (request != null) {
            request.setId(id);
        }
        ValidatorDto validation = fleetComplianceServiceValidator.isEditFleetComplianceRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetComplianceRecord record = fleetComplianceRecordRepository
                .findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (record == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        if (request.getFileUploadId() != null) {
            if (!fleetFileUploadHelper.validateFileUploadReference(
                    request.getFileUploadId(), record.getSubjectType(), record.getSubjectId(), locale)) {
                return errorResponse(400, fleetFileUploadHelper.fileUploadInvalidMessage(locale));
            }
            record.setFileUploadId(request.getFileUploadId());
        }

        if (request.getExpiresAt() != null) {
            record.setExpiresAt(request.getExpiresAt());
        } else if (request.getFileUploadId() != null) {
            record.setExpiresAt(fleetFileUploadHelper.resolveExpiresAt(null, request.getFileUploadId()));
        }

        if (request.getNotes() != null) {
            record.setNotes(request.getNotes());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            record.setStatus(ComplianceStatus.valueOf(request.getStatus().trim().toUpperCase()));
        } else {
            record.setStatus(ComplianceStatusResolver.resolve(
                    record.getExpiresAt(), record.getFileUploadId(), defaultExpiringSoonDays));
        }

        record.setModifiedAt(LocalDateTime.now());
        record.setModifiedBy(username);

        FleetComplianceRecord saved = fleetComplianceRecordRepository.save(record);
        FleetComplianceRecordResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetComplianceRecordDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetComplianceRecordResponse listExpiring(int withinDays, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        int days = withinDays > 0 ? withinDays : defaultExpiringSoonDays;
        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        List<FleetComplianceRecord> records = fleetComplianceRecordRepository.findExpiringByOrganizationId(
                organizationId, threshold, EntityStatus.DELETED);
        FleetComplianceRecordResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_COMPLIANCE_EXPIRING_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetComplianceRecordDtoList(records.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    private boolean subjectExists(Long organizationId, ComplianceSubjectType subjectType, Long subjectId) {
        return switch (subjectType) {
            case ASSET -> fleetAssetRepository
                    .findByIdAndOrganizationIdAndEntityStatusNot(subjectId, organizationId, EntityStatus.DELETED)
                    .isPresent();
            case DRIVER -> fleetDriverRepository
                    .findByIdAndOrganizationIdAndEntityStatusNot(subjectId, organizationId, EntityStatus.DELETED)
                    .isPresent();
        };
    }

    private FleetComplianceRecordResponse successResponse(int statusCode, String message) {
        FleetComplianceRecordResponse response = new FleetComplianceRecordResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private FleetComplianceRecordResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private FleetComplianceRecordResponse errorResponse(int statusCode, String message, List<String> errors) {
        FleetComplianceRecordResponse response = new FleetComplianceRecordResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
