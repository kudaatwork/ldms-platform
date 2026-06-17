package projectlx.trip.tracking.business.logic.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * MySQL advisory lock — serialises trip start per shipment across concurrent requests.
 */
@Component
public class ShipmentTripStartLock {

    private static final int LOCK_TIMEOUT_SECONDS = 15;

    @PersistenceContext
    private EntityManager entityManager;

    public boolean tryLock(Long shipmentId) {
        if (shipmentId == null || shipmentId < 1) {
            return false;
        }
        Number result = (Number) entityManager
                .createNativeQuery("SELECT GET_LOCK(:lockName, :timeout)")
                .setParameter("lockName", lockName(shipmentId))
                .setParameter("timeout", LOCK_TIMEOUT_SECONDS)
                .getSingleResult();
        return result != null && result.intValue() == 1;
    }

    public void unlock(Long shipmentId) {
        if (shipmentId == null || shipmentId < 1) {
            return;
        }
        entityManager
                .createNativeQuery("SELECT RELEASE_LOCK(:lockName)")
                .setParameter("lockName", lockName(shipmentId))
                .getSingleResult();
    }

    private static String lockName(Long shipmentId) {
        return "ldms-trip-start-shipment-" + shipmentId;
    }
}
