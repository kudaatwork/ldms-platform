package projectlx.co.zw.organizationmanagement.clients;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Placeholder Feign client for user-management service.
 */
@FeignClient(name = "user-management-service")
public interface UserManagementServiceClient {
}
