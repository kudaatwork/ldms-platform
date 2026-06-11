package projectlx.inventory.management.utils.requests;

/**
 * Thin adapter request for reserving inventory that aliases the existing
 * CreateSalesReservationRequest. This allows a more generic API name
 * while reusing the established reservation workflow and validation.
 */
public class CreateReservationRequest extends CreateSalesReservationRequest {
}
