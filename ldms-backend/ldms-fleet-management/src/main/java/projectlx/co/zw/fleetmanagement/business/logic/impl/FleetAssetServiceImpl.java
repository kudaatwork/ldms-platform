package projectlx.co.zw.fleetmanagement.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetAssetService;
import projectlx.co.zw.fleetmanagement.business.logic.support.CallerOrganizationResolver;
import projectlx.co.zw.fleetmanagement.business.logic.support.FleetMapper;
import projectlx.co.zw.fleetmanagement.business.validation.api.FleetAssetServiceValidator;
import projectlx.co.zw.fleetmanagement.model.FleetAsset;
import projectlx.co.zw.fleetmanagement.repository.FleetAssetRepository;
import projectlx.co.zw.fleetmanagement.utils.enums.FleetAssetStatus;
import projectlx.co.zw.fleetmanagement.utils.enums.FleetAssetType;
import projectlx.co.zw.fleetmanagement.utils.enums.FleetOwnershipType;
import projectlx.co.zw.fleetmanagement.utils.enums.I18Code;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetAssetResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FleetAssetServiceImpl implements FleetAssetService {

    private final FleetAssetServiceValidator fleetAssetServiceValidator;
    private final FleetAssetRepository fleetAssetRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public FleetAssetResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        List<FleetAsset> assets = fleetAssetRepository.findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
                organizationId, EntityStatus.DELETED);
        FleetAssetResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetAssetDtoList(assets.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse create(CreateFleetAssetRequest request, Locale locale, String username) {
        ValidatorDto validation = fleetAssetServiceValidator.isCreateFleetAssetRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetAsset asset = new FleetAsset();
        asset.setOrganizationId(organizationId);
        mapRequestToEntity(request, asset);
        asset.setEntityStatus(EntityStatus.ACTIVE);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setCreatedBy(username);

        FleetAsset saved = fleetAssetRepository.save(asset);
        FleetAssetResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_ASSET_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetAssetDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse update(Long id, EditFleetAssetRequest request, Locale locale, String username) {
        if (request != null) {
            request.setId(id);
        }
        ValidatorDto validation = fleetAssetServiceValidator.isEditFleetAssetRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetAsset asset = fleetAssetRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (asset == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        mapEditRequestToEntity(request, asset);
        asset.setModifiedAt(LocalDateTime.now());
        asset.setModifiedBy(username);

        FleetAsset saved = fleetAssetRepository.save(asset);
        FleetAssetResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetAssetDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse delete(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetAsset asset = fleetAssetRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (asset == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        asset.setEntityStatus(EntityStatus.DELETED);
        asset.setModifiedAt(LocalDateTime.now());
        asset.setModifiedBy(username);
        fleetAssetRepository.save(asset);

        return successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_DELETE_SUCCESS.getCode(), new String[]{}, locale));
    }

    private void mapRequestToEntity(CreateFleetAssetRequest request, FleetAsset asset) {
        applyAssetFields(request.getAssetType(), request.getOwnershipType(), request.getContractedTransporterOrganizationId(),
                request.getRegistration(), request.getMakeModel(), request.getStatus(), request.getDriverName(),
                request.getUtilizationPct(), asset);
    }

    private void mapEditRequestToEntity(EditFleetAssetRequest request, FleetAsset asset) {
        applyAssetFields(request.getAssetType(), request.getOwnershipType(), request.getContractedTransporterOrganizationId(),
                request.getRegistration(), request.getMakeModel(), request.getStatus(), request.getDriverName(),
                request.getUtilizationPct(), asset);
    }

    private void applyAssetFields(String assetType, String ownershipType, Long contractedTransporterOrganizationId,
                                  String registration, String makeModel, String status, String driverName,
                                  BigDecimal utilizationPct, FleetAsset asset) {
        asset.setAssetType(FleetAssetType.valueOf(assetType.trim().toUpperCase()));
        asset.setOwnershipType(FleetOwnershipType.valueOf(ownershipType.trim().toUpperCase()));
        asset.setContractedTransporterOrganizationId(contractedTransporterOrganizationId);
        asset.setRegistration(registration.trim());
        asset.setMakeModel(makeModel.trim());
        if (status != null && !status.isBlank()) {
            asset.setStatus(FleetAssetStatus.valueOf(status.trim().toUpperCase()));
        }
        asset.setDriverName(driverName);
        asset.setUtilizationPct(utilizationPct != null ? utilizationPct : BigDecimal.ZERO);
    }

    private FleetAssetResponse successResponse(int statusCode, String message) {
        FleetAssetResponse response = new FleetAssetResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private FleetAssetResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private FleetAssetResponse errorResponse(int statusCode, String message, List<String> errors) {
        FleetAssetResponse response = new FleetAssetResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
