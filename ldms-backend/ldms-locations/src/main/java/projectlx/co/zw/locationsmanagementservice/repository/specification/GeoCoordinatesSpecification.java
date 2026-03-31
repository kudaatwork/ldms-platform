package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;

public class GeoCoordinatesSpecification {

    public static Specification<GeoCoordinates> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(GeoCoordinates_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<GeoCoordinates> latitudeEquals(final BigDecimal latitude) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(GeoCoordinates_.latitude), latitude);
            return p;
        };
    }

    public static Specification<GeoCoordinates> longitudeEquals(final BigDecimal longitude) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(GeoCoordinates_.longitude), longitude);
            return p;
        };
    }

    public static Specification<GeoCoordinates> latitudeBetween(final BigDecimal minLatitude, final BigDecimal maxLatitude) {
        return (root, query, cb) -> {
            Predicate p = cb.between(root.get(GeoCoordinates_.latitude), minLatitude, maxLatitude);
            return p;
        };
    }

    public static Specification<GeoCoordinates> longitudeBetween(final BigDecimal minLongitude, final BigDecimal maxLongitude) {
        return (root, query, cb) -> {
            Predicate p = cb.between(root.get(GeoCoordinates_.longitude), minLongitude, maxLongitude);
            return p;
        };
    }

    public static Specification<GeoCoordinates> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(GeoCoordinates_.entityStatus), entityStatus);
            return p;
        };
    }
}