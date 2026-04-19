package projectlx.co.zw.organizationmanagement.clients;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Placeholder Feign client for locations service.
 */
@FeignClient(name = "ldms-locations")
public interface LocationsServiceClient {
}
