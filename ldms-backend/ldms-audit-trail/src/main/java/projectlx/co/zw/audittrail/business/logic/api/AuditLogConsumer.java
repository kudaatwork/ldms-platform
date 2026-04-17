package projectlx.co.zw.audittrail.business.logic.api;

import projectlx.co.zw.audittrail.utils.dtos.AuditLogPayload;

/**
 * Persists inbound audit payloads. AMQP delivery semantics are handled in {@link
 * projectlx.co.zw.audittrail.business.logic.impl.AuditLogConsumerImpl}.
 */
public interface AuditLogConsumer {

    void processInboundAuditLog(AuditLogPayload payload);
}
