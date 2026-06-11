package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateSalesOrderRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderRequest;
import projectlx.inventory.management.utils.requests.FulfillSalesOrderRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface SalesOrderServiceValidator {
    ValidatorDto isCreateSalesOrderRequestValid(CreateSalesOrderRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditSalesOrderRequest request, Locale locale);

    ValidatorDto isFulfillOrderRequestValid(FulfillSalesOrderRequest request, Locale locale);
}
