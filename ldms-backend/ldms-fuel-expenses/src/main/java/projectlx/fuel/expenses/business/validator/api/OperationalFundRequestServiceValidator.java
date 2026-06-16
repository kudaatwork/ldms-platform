package projectlx.fuel.expenses.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.fuel.expenses.utils.requests.ApproveFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CancelFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CreateFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.FundRequestFilterRequest;
import projectlx.fuel.expenses.utils.requests.RejectFundRequestRequest;

import java.util.Locale;

public interface OperationalFundRequestServiceValidator {

    ValidatorDto isCreateFundRequestValid(CreateFundRequestRequest request, Locale locale);

    ValidatorDto isApproveFundRequestValid(ApproveFundRequestRequest request, Locale locale);

    ValidatorDto isRejectFundRequestValid(RejectFundRequestRequest request, Locale locale);

    ValidatorDto isCancelFundRequestValid(CancelFundRequestRequest request, Locale locale);

    ValidatorDto isFindByIdRequestValid(Long id, Locale locale);

    ValidatorDto isFindByMultipleFiltersRequestValid(FundRequestFilterRequest request, Locale locale);
}
