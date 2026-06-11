package projectlx.inventory.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.service.processor.api.PurchaseReturnServiceProcessor;
import projectlx.inventory.management.utils.requests.CreatePurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.PurchaseReturnMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.PurchaseReturnResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/purchase-return")
@Tag(name = "Purchase Return System Resource", description = "Operations related to managing purchase returns (system)")
@RequiredArgsConstructor
public class PurchaseReturnSystemResource {

    private final PurchaseReturnServiceProcessor purchaseReturnServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseReturnSystemResource.class);

    @Auditable(action = "CREATE_PURCHASE_RETURN")
    @PostMapping("/create")
    @Operation(summary = "Create a new purchase return", description = "Creates a new purchase return and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase return created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public PurchaseReturnResponse create(@Valid @RequestBody final CreatePurchaseReturnRequest request,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                         final Locale locale) {
        return purchaseReturnServiceProcessor.create(request, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_PURCHASE_RETURN")
    @PutMapping("/update")
    @Operation(summary = "Update purchase return details", description = "Updates an existing purchase return's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase return updated successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase return not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public PurchaseReturnResponse update(@Valid @RequestBody final EditPurchaseReturnRequest request,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                         final Locale locale) {
        return purchaseReturnServiceProcessor.update(request, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_PURCHASE_RETURN_BY_ID")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find purchase return by ID", description = "Retrieves a purchase return by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase return found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase return not found"),
            @ApiResponse(responseCode = "400", description = "Purchase return id supplied invalid")
    })
    public PurchaseReturnResponse findById(@PathVariable("id") final Long id,
                                           @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                           final Locale locale) {
        return purchaseReturnServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_PURCHASE_RETURN")
    @DeleteMapping("/delete-by-id/{id}")
    @Operation(summary = "Delete a purchase return by ID")
    public PurchaseReturnResponse delete(@PathVariable("id") final Long id,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                         final Locale locale) {
        return purchaseReturnServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_PURCHASE_RETURNS_BY_LIST")
    @GetMapping("/find-by-list")
    @Operation(summary = "Get all purchase returns", description = "Retrieves a list of all purchase returns")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase returns retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase return(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching purchase returns")
    })
    public PurchaseReturnResponse findAllAsAList(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseReturnServiceProcessor.findAllAsList(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_PURCHASE_RETURNS_BY_MULTIPLE_FILTERS")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find purchase returns by multiple filters",
            description = "Retrieves a list of purchase returns that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase return(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase return(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public PurchaseReturnResponse findByMultipleFilters(
            @Valid @RequestBody PurchaseReturnMultipleFiltersRequest filters,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseReturnServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
    }
}