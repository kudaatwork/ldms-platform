package projectlx.fuel.expenses.business.logic.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.business.logic.api.RoadsideProviderService;
import projectlx.fuel.expenses.business.logic.support.FuelExpensesMapper;
import projectlx.fuel.expenses.business.logic.support.RoadsideGeoSupport;
import projectlx.fuel.expenses.model.RoadsideProvider;
import projectlx.fuel.expenses.repository.RoadsideProviderRepository;
import projectlx.fuel.expenses.utils.dtos.RoadsideProviderDto;
import projectlx.fuel.expenses.utils.responses.RoadsideProviderResponse;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RoadsideProviderServiceImpl implements RoadsideProviderService {

    private final RoadsideProviderRepository roadsideProviderRepository;
    private final FuelExpensesMapper fuelExpensesMapper;

    public RoadsideProviderServiceImpl(RoadsideProviderRepository roadsideProviderRepository,
                                       FuelExpensesMapper fuelExpensesMapper) {
        this.roadsideProviderRepository = roadsideProviderRepository;
        this.fuelExpensesMapper = fuelExpensesMapper;
    }

    @Override
    public RoadsideProviderResponse listAll(Locale locale) {
        List<RoadsideProviderDto> rows = roadsideProviderRepository
                .findByEntityStatusNotOrderByNameAsc(EntityStatus.DELETED)
                .stream()
                .map(fuelExpensesMapper::toRoadsideProviderDto)
                .toList();
        RoadsideProviderResponse response = success(200, "Roadside providers loaded");
        response.setRoadsideProviderDtoList(rows);
        return response;
    }

    @Override
    public RoadsideProviderResponse listNearby(double latitude, double longitude, double radiusKm, Locale locale) {
        double safeRadius = radiusKm > 0 ? Math.min(radiusKm, 500) : 150;
        BigDecimal[] box = RoadsideGeoSupport.boundingBox(latitude, longitude, safeRadius);
        List<RoadsideProvider> candidates = roadsideProviderRepository.findInBoundingBox(
                box[0], box[1], box[2], box[3]);

        List<RoadsideProviderDto> rows = candidates.stream()
                .map(provider -> {
                    RoadsideProviderDto dto = fuelExpensesMapper.toRoadsideProviderDto(provider);
                    dto.setDistanceKm(RoadsideGeoSupport.distanceKm(
                            latitude,
                            longitude,
                            provider.getLatitude().doubleValue(),
                            provider.getLongitude().doubleValue()));
                    return dto;
                })
                .filter(dto -> dto.getDistanceKm() != null && dto.getDistanceKm() <= safeRadius)
                .sorted(Comparator.comparing(RoadsideProviderDto::getDistanceKm))
                .toList();

        RoadsideProviderResponse response = success(200, "Nearby roadside providers loaded");
        response.setRoadsideProviderDtoList(rows);
        return response;
    }

    private RoadsideProviderResponse success(int statusCode, String message) {
        RoadsideProviderResponse response = new RoadsideProviderResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }
}
