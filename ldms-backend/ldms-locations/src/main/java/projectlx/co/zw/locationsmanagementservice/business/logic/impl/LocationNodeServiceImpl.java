package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocationNodeServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationNodeService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LocationNodeServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.LocationAlias;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.repository.LocationNodeRepository;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class LocationNodeServiceImpl implements LocationNodeService {
    private static final String EXCHANGE = "ldms.locations.exchange";
    private static final String CREATED_ROUTING_KEY = "location.node.created";
    private static final String UPDATED_ROUTING_KEY = "location.node.updated";

    private final LocationNodeServiceValidator validator;
    private final LocationNodeRepository locationNodeRepository;
    private final LocationNodeServiceAuditable locationNodeServiceAuditable;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public LocationNodeResponse create(CreateLocationNodeRequest request, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isCreateValid(request, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid location create request", null, null, null, validation.getErrorMessages());
        }

        LocationNode node = new LocationNode();
        node.setName(request.getName().trim());
        node.setCode(request.getCode());
        node.setLocationType(request.getLocationType());
        node.setLatitude(request.getLatitude());
        node.setLongitude(request.getLongitude());
        node.setTimezone(request.getTimezone());
        node.setPostalCode(request.getPostalCode());
        node.setCreatedAt(LocalDateTime.now());
        node.setCreatedBy(username);
        node.setEntityStatus(EntityStatus.ACTIVE);

        if (request.getParentId() != null) {
            Optional<LocationNode> parent = locationNodeRepository.findByIdAndEntityStatusNot(request.getParentId(), EntityStatus.DELETED);
            if (parent.isEmpty()) {
                return response(404, false, "Parent location not found", null, null, null, null);
            }
            node.setParent(parent.get());
        }
        node.setAliases(toAliases(request.getAliases(), node, username));

        LocationNode saved = locationNodeServiceAuditable.create(node, locale, username);
        publishEvent(CREATED_ROUTING_KEY, saved.getId());
        return response(201, true, "Location node created successfully", toDto(saved), null, null, null);
    }

    @Override
    public LocationNodeResponse update(EditLocationNodeRequest request, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isEditValid(request, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid location update request", null, null, null, validation.getErrorMessages());
        }
        Optional<LocationNode> existing = locationNodeRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (existing.isEmpty()) {
            return response(404, false, "Location node not found", null, null, null, null);
        }

        LocationNode node = existing.get();
        node.setName(request.getName().trim());
        node.setCode(request.getCode());
        node.setLocationType(request.getLocationType());
        node.setLatitude(request.getLatitude());
        node.setLongitude(request.getLongitude());
        node.setTimezone(request.getTimezone());
        node.setPostalCode(request.getPostalCode());
        node.setModifiedAt(LocalDateTime.now());
        node.setModifiedBy(username);

        if (request.getParentId() != null) {
            Optional<LocationNode> parent = locationNodeRepository.findByIdAndEntityStatusNot(request.getParentId(), EntityStatus.DELETED);
            if (parent.isEmpty()) {
                return response(404, false, "Parent location not found", null, null, null, null);
            }
            node.setParent(parent.get());
        } else {
            node.setParent(null);
        }

        node.getAliases().clear();
        node.getAliases().addAll(toAliases(request.getAliases(), node, username));
        LocationNode saved = locationNodeServiceAuditable.update(node, locale, username);
        publishEvent(UPDATED_ROUTING_KEY, saved.getId());
        return response(200, true, "Location node updated successfully", toDto(saved), null, null, null);
    }

    @Override
    public LocationNodeResponse findById(Long id, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid id", null, null, null, validation.getErrorMessages());
        }
        return locationNodeRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .map(node -> response(200, true, "Location node found", toDto(node), null, null, null))
                .orElseGet(() -> response(404, false, "Location node not found", null, null, null, null));
    }

    @Override
    public LocationNodeResponse findByParentId(Long parentId, java.util.Locale locale, String username) {
        List<LocationNodeDto> dtoList = locationNodeRepository.findByParentIdAndEntityStatusNot(parentId, EntityStatus.DELETED)
                .stream().map(this::toDto).collect(Collectors.toList());
        return response(200, true, "Location nodes retrieved successfully", null, dtoList, null, null);
    }

    @Override
    public LocationNodeResponse findByMultipleFilters(LocationNodeMultipleFiltersRequest request, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isFilterValid(request, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid filter request", null, null, null, validation.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<LocationNode> page;
        if (request.getSearchValue() != null && !request.getSearchValue().isBlank()) {
            page = locationNodeRepository.findByNameContainingIgnoreCaseAndEntityStatusNot(request.getSearchValue().trim(), EntityStatus.DELETED, pageable);
        } else {
            page = locationNodeRepository.findAll(pageable);
        }

        List<LocationNode> filtered = page.getContent().stream()
                .filter(node -> node.getEntityStatus() != EntityStatus.DELETED)
                .filter(node -> request.getLocationType() == null || request.getLocationType() == node.getLocationType())
                .filter(node -> request.getParentId() == null || (node.getParent() != null && request.getParentId().equals(node.getParent().getId())))
                .collect(Collectors.toList());
        List<LocationNodeDto> dtoList = filtered.stream().map(this::toDto).collect(Collectors.toList());
        return response(200, true, "Location nodes retrieved successfully", null, null, new PageImpl<>(dtoList, pageable, page.getTotalElements()), null);
    }

    @Override
    public LocationNodeResponse delete(Long id, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid id", null, null, null, validation.getErrorMessages());
        }
        Optional<LocationNode> existing = locationNodeRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (existing.isEmpty()) {
            return response(404, false, "Location node not found", null, null, null, null);
        }
        LocationNode node = existing.get();
        node.setEntityStatus(EntityStatus.DELETED);
        node.setModifiedAt(LocalDateTime.now());
        node.setModifiedBy(username);
        locationNodeServiceAuditable.delete(node, locale, username);
        return response(200, true, "Location node deleted successfully", toDto(node), null, null, null);
    }

    private List<LocationAlias> toAliases(List<String> aliases, LocationNode node, String username) {
        List<LocationAlias> aliasEntities = new ArrayList<>();
        if (aliases == null) {
            return aliasEntities;
        }
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            LocationAlias a = new LocationAlias();
            a.setAlias(alias.trim());
            a.setLocationNode(node);
            a.setEntityStatus(EntityStatus.ACTIVE);
            a.setCreatedAt(LocalDateTime.now());
            a.setCreatedBy(username);
            aliasEntities.add(a);
        }
        return aliasEntities;
    }

    private LocationNodeDto toDto(LocationNode node) {
        LocationNodeDto dto = new LocationNodeDto();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setCode(node.getCode());
        dto.setLocationType(node.getLocationType());
        if (node.getParent() != null) {
            dto.setParentId(node.getParent().getId());
            dto.setParentName(node.getParent().getName());
        }
        dto.setLatitude(node.getLatitude());
        dto.setLongitude(node.getLongitude());
        dto.setTimezone(node.getTimezone());
        dto.setPostalCode(node.getPostalCode());
        dto.setAliases(node.getAliases().stream().map(LocationAlias::getAlias).collect(Collectors.toList()));
        dto.setEntityStatus(node.getEntityStatus());
        dto.setCreatedAt(node.getCreatedAt());
        dto.setCreatedBy(node.getCreatedBy());
        dto.setModifiedAt(node.getModifiedAt());
        dto.setModifiedBy(node.getModifiedBy());
        return dto;
    }

    private LocationNodeResponse response(int status, boolean success, String message, LocationNodeDto dto, List<LocationNodeDto> dtoList,
                                          Page<LocationNodeDto> dtoPage, List<String> errors) {
        LocationNodeResponse response = new LocationNodeResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setLocationNodeDto(dto);
        response.setLocationNodeDtoList(dtoList);
        response.setLocationNodeDtoPage(dtoPage);
        response.setErrorMessages(errors);
        return response;
    }

    private void publishEvent(String routingKey, Long nodeId) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, "locationNodeId=" + nodeId);
        } catch (Exception ex) {
            log.warn("Failed to publish location event for node {}: {}", nodeId, ex.getMessage());
        }
    }
}
