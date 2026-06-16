package projectlx.fuel.expenses.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fuel.expenses.business.logic.api.OperationalFundRequestService;
import projectlx.fuel.expenses.service.processor.api.OperationalFundRequestServiceProcessor;
import projectlx.fuel.expenses.utils.requests.ApproveFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CancelFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CreateFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.FundRequestFilterRequest;
import projectlx.fuel.expenses.utils.requests.RejectFundRequestRequest;
import projectlx.fuel.expenses.utils.responses.OperationalFundRequestResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class OperationalFundRequestServiceProcessorImpl implements OperationalFundRequestServiceProcessor {

    private final OperationalFundRequestService operationalFundRequestService;

    @Override
    public OperationalFundRequestResponse create(CreateFundRequestRequest request, Locale locale, String username) {
        log.info("Processing create fund request for tripId={} by user={}", request.getTripId(), username);
        return operationalFundRequestService.create(request, locale, username);
    }

    @Override
    public OperationalFundRequestResponse approve(ApproveFundRequestRequest request, Locale locale, String username) {
        log.info("Processing approve fund request id={} by user={}", request.getRequestId(), username);
        return operationalFundRequestService.approve(request, locale, username);
    }

    @Override
    public OperationalFundRequestResponse reject(RejectFundRequestRequest request, Locale locale, String username) {
        log.info("Processing reject fund request id={} by user={}", request.getRequestId(), username);
        return operationalFundRequestService.reject(request, locale, username);
    }

    @Override
    public OperationalFundRequestResponse cancel(CancelFundRequestRequest request, Locale locale, String username) {
        log.info("Processing cancel fund request id={} by user={}", request.getRequestId(), username);
        return operationalFundRequestService.cancel(request, locale, username);
    }

    @Override
    public OperationalFundRequestResponse findById(Long id, Locale locale, String username) {
        log.info("Processing findById fund request id={} by user={}", id, username);
        return operationalFundRequestService.findById(id, locale, username);
    }

    @Override
    public OperationalFundRequestResponse findByMultipleFilters(FundRequestFilterRequest request, Locale locale,
            String username) {
        log.info("Processing findByMultipleFilters for fund requests by user={}", username);
        return operationalFundRequestService.findByMultipleFilters(request, locale, username);
    }

    @Override
    public OperationalFundRequestResponse completeRoadsideStop(Long tripId, Locale locale, String username) {
        log.info("Processing complete roadside stop for tripId={} by user={}", tripId, username);
        return operationalFundRequestService.completeRoadsideStop(tripId, locale, username);
    }
}
