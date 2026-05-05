package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.model.Address_;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class AddressSpecification {

    public static Specification<Address> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(Address_.entityStatus)),
                cb.notEqual(root.get(Address_.entityStatus), excludedStatus));
    }

    public static Specification<Address> line1Like(final String line1) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.line1).as(String.class), "%" + line1 + "%");
            return p;
        };
    }

    public static Specification<Address> line2Like(final String line2) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.line2).as(String.class), "%" + line2 + "%");
            return p;
        };
    }

    public static Specification<Address> postalCodeLike(final String postalCode) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.postalCode).as(String.class), postalCode + "%");
            return p;
        };
    }


    public static Specification<Address> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(Address_.line1), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Address_.line2), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Address_.postalCode), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<Address> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Address_.entityStatus), entityStatus);
            return p;
        };
    }

    public static Specification<Address> hasSettlementType(final SettlementType settlementType) {
        return (root, query, cb) -> cb.equal(root.get(Address_.settlementType), settlementType);
    }

    public static Specification<Address> hasSuburbId(final Long suburbId) {
        return (root, query, cb) -> cb.equal(root.get("suburb").get("id"), suburbId);
    }

    public static Specification<Address> hasVillageId(final Long villageId) {
        return (root, query, cb) -> cb.equal(root.get("villageLocationNode").get("id"), villageId);
    }

    public static Specification<Address> hasSettlementId(final Long settlementId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("suburb").get("id"), settlementId),
                cb.equal(root.get("villageLocationNode").get("id"), settlementId)
        );
    }

    public static Specification<Address> hasCityId(final Long cityId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("suburb").get("cityLocationNode").get("id"), cityId),
                cb.equal(root.get("villageLocationNode").get("parent").get("id"), cityId)
        );
    }

    public static Specification<Address> hasDistrictId(final Long districtId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("suburb").get("district").get("id"), districtId),
                cb.equal(root.get("villageLocationNode").get("district").get("id"), districtId)
        );
    }

    public static Specification<Address> hasProvinceId(final Long provinceId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("suburb").get("district").get("province").get("id"), provinceId),
                cb.equal(root.get("villageLocationNode").get("district").get("province").get("id"), provinceId)
        );
    }

    public static Specification<Address> hasCountryId(final Long countryId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("suburb").get("district").get("province").get("country").get("id"), countryId),
                cb.equal(root.get("villageLocationNode").get("district").get("province").get("country").get("id"), countryId)
        );
    }
}
