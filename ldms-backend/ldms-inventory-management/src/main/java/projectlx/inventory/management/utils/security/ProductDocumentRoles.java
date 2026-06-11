package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProductDocumentRoles {

    CREATE_PRODUCT_DOCUMENT("CREATE_PRODUCT_DOCUMENT", "Creates product document"),
    DELETE_PRODUCT_DOCUMENT("DELETE_PRODUCT_DOCUMENT", "Deletes product document"),
    UPDATE_PRODUCT_DOCUMENT("UPDATE_PRODUCT_DOCUMENT", "Updates product document information"),
    VIEW_PRODUCT_DOCUMENT_BY_ID("VIEW_PRODUCT_DOCUMENT_BY_ID", "Views product document by id"),
    VIEW_ALL_PRODUCT_DOCUMENTS_AS_A_LIST("VIEW_ALL_PRODUCT_DOCUMENTS_AS_A_LIST", "Views all product documents as a list"),
    VIEW_ALL_PRODUCT_DOCUMENTS_BY_MULTIPLE_FILTERS("VIEW_ALL_PRODUCT_DOCUMENTS_BY_MULTIPLE_FILTERS", "Views all product documents by multiple filters"),
    EXPORT_PRODUCT_DOCUMENTS("EXPORT_PRODUCT_DOCUMENTS", "Exports product documents"),
    IMPORT_PRODUCT_DOCUMENTS("IMPORT_PRODUCT_DOCUMENTS", "Imports product documents");

    private final String roleName;
    private final String description;
}
