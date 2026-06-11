package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.IdempotencyService;
import projectlx.inventory.management.model.IdempotencyKey;
import projectlx.inventory.management.model.IdempotencyOperation;
import projectlx.inventory.management.model.IdempotencyStatus;
import projectlx.inventory.management.repository.IdempotencyKeyRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository repository;

    @Override
    @Transactional
    public boolean tryAcquire(String key, IdempotencyOperation operation, String referenceType) {
        
        if (key == null || key.isBlank()) {
            return true; // no key provided, proceed without idempotency
        }
        try {
            IdempotencyKey idempotencyKey = new IdempotencyKey();
            idempotencyKey.setKeyValue(key);
            idempotencyKey.setOperation(operation);
            idempotencyKey.setReferenceType(referenceType);
            idempotencyKey.setStatus(IdempotencyStatus.IN_PROGRESS);
            repository.save(idempotencyKey);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Unique constraint violation means key already exists
            log.info("Idempotency key already exists: {} - treating as duplicate request", key);
            return false;
        }
    }

    @Override
    @Transactional
    public void markProcessed(String key, Long referenceId) {

        if (key == null || key.isBlank()) return;

        repository.findByKeyValueAndEntityStatusNot(key, EntityStatus.DELETED).ifPresent(idempotencyKey -> {
            idempotencyKey.setStatus(IdempotencyStatus.PROCESSED);
            idempotencyKey.setReferenceId(referenceId);
            repository.save(idempotencyKey);
        });
    }

    @Override
    @Transactional
    public void markProcessedWithResponse(String key, Long referenceId, Integer responseStatusCode, String responseBodyJson) {
        if (key == null || key.isBlank()) return;
        repository.findByKeyValueAndEntityStatusNot(key, EntityStatus.DELETED).ifPresent(idempotencyKey -> {
            idempotencyKey.setStatus(IdempotencyStatus.PROCESSED);
            idempotencyKey.setReferenceId(referenceId);
            idempotencyKey.setResponseStatusCode(responseStatusCode);
            idempotencyKey.setResponseBody(responseBodyJson);
            repository.save(idempotencyKey);
        });
    }

    @Override
    public Optional<IdempotencyKey> findByKey(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        return repository.findByKeyValueAndEntityStatusNot(key, EntityStatus.DELETED);
    }
}
