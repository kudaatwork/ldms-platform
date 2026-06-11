package projectlx.inventory.management.business.logic.api;

import java.util.Optional;

import projectlx.inventory.management.model.IdempotencyKey;
import projectlx.inventory.management.model.IdempotencyOperation;

public interface IdempotencyService {
    /**
     * Try to acquire an idempotency key by inserting a new record. If the key already exists, returns false.
     */
    boolean tryAcquire(String key, IdempotencyOperation operation, String referenceType);

    /**
     * Mark an idempotency key as processed and store reference id.
     */
    void markProcessed(String key, Long referenceId);

    /**
     * Mark an idempotency key as processed and cache the response for replay.
     */
    void markProcessedWithResponse(String key, Long referenceId, Integer responseStatusCode, String responseBodyJson);

    /**
     * Find an idempotency key by value.
     */
    Optional<IdempotencyKey> findByKey(String key);
}
