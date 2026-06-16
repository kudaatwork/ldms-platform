package projectlx.fuel.expenses.business.logic.api;

import projectlx.fuel.expenses.utils.requests.ApproveFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CancelFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CreateFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.FundRequestFilterRequest;
import projectlx.fuel.expenses.utils.requests.RejectFundRequestRequest;
import projectlx.fuel.expenses.utils.responses.OperationalFundRequestResponse;

import java.util.Locale;

public interface OperationalFundRequestService {

    OperationalFundRequestResponse create(CreateFundRequestRequest request, Locale locale, String username);

    OperationalFundRequestResponse approve(ApproveFundRequestRequest request, Locale locale, String username);

    OperationalFundRequestResponse reject(RejectFundRequestRequest request, Locale locale, String username);

    OperationalFundRequestResponse cancel(CancelFundRequestRequest request, Locale locale, String username);

    OperationalFundRequestResponse findById(Long id, Locale locale, String username);

    OperationalFundRequestResponse findByMultipleFilters(FundRequestFilterRequest request, Locale locale,
            String username);

    /**
     * Marks a roadside stop as complete by recording a ROADSIDE_RESUMED event on the trip.
     * Called by driver/fleet manager after the mechanic work or fuel fill has finished.
     */
    OperationalFundRequestResponse completeRoadsideStop(Long tripId, Locale locale, String username);
}
