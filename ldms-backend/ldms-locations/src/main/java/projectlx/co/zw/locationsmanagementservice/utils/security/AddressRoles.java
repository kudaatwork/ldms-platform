package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AddressRoles {

    CREATE_ADDRESS("CREATE_ADDRESS", "Creates address"),
    UPDATE_ADDRESS("UPDATE_ADDRESS", "Updates address information"),
    VIEW_ADDRESS_BY_ID("VIEW_ADDRESS_BY_ID", "Views address by id"),
    DELETE_ADDRESS("DELETE_ADDRESS", "Deletes address"),
    VIEW_ALL_ADDRESSES_AS_A_LIST("VIEW_ALL_ADDRESSES_AS_A_LIST", "Views all addresses as a list"),
    VIEW_ALL_ADDRESSES_BY_MULTIPLE_FILTERS("VIEW_ALL_ADDRESSES_BY_MULTIPLE_FILTERS", "Views all addresses by multiple filters"),
    EXPORT_ADDRESSES("EXPORT_ADDRESSES", "Exports addresses"),
    IMPORT_ADDRESSES("IMPORT_ADDRESSES", "Imports addresses from CSV");

    private final String roleName;
    private final String description;
}