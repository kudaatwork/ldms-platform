package projectlx.fuel.expenses.business.auditable.api;

import projectlx.fuel.expenses.model.OperationalFundRequest;

import java.util.Locale;

public interface OperationalFundRequestServiceAuditable {

    OperationalFundRequest create(OperationalFundRequest request, Locale locale, String username);

    OperationalFundRequest update(OperationalFundRequest request, Locale locale, String username);
}
