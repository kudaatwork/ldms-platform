package projectlx.co.zw.notifications.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_NOTIFICATION_REQUEST_IS_NULL("message.notification.request.isNull"),

    // Audit log validation messages
    AUDIT_LOG_NULL("audit.log.null"),
    AUDIT_LOG_ACTION_MISSING("audit.log.action.missing"),
    AUDIT_LOG_USERNAME_MISSING("audit.log.username.missing"),
    AUDIT_LOG_EVENT_TYPE_MISSING("audit.log.event.type.missing"),

    // Notification validation messages
    NOTIFICATION_REQUEST_NULL("notification.request.null"),
    NOTIFICATION_TEMPLATE_NULL("notification.template.null"),
    NOTIFICATION_RECIPIENT_MISSING("notification.recipient.missing"),
    EMAIL_CONTENT_IS_MISSING("email.content.is.missing"),
    SMS_CONTENT_IS_MISSING("sms.content.is.missing"),
    WHATSAPP_TEMPLATE_NAME_IS_MISSING("whatsapp.template.name.is.missing"),
    IN_APP_CONTENT_IS_MISSING("in.app.content.is.missing"),
    CHANNEL_LIST_IS_EMPTY_OR_NULL("channel.list.is.empty.or.null"),
    DESCRIPTION_MISSING("notification.data.missing"),
    NOTIFICATION_TEMPLATE_KEY_MISSING("notification.template.key.missing"),
    NOTIFICATION_RECIPIENT_CONTACT_MISSING("notification.recipient.contact.missing"),

    // Template processor validation messages
    TEMPLATE_CONTENT_MISSING("template.content.missing"),
    TEMPLATE_DATA_NULL("template.data.null"),

    // Template service messages
    TEMPLATE_KEY_ALREADY_EXISTS("template.key.already.exists"),
    TEMPLATE_CREATED_SUCCESSFULLY("template.created.successfully"),
    TEMPLATE_INVALID_ID("template.invalid.id"),
    TEMPLATE_NOT_FOUND("template.not.found"),
    TEMPLATE_RETRIEVED_SUCCESSFULLY("template.retrieved.successfully"),
    TEMPLATES_RETRIEVED_SUCCESSFULLY("templates.retrieved.successfully"),
    NO_TEMPLATES_FOUND("no.templates.found"),
    TEMPLATE_INVALID_UPDATE_REQUEST("template.invalid.update.request"),
    TEMPLATE_UPDATED_SUCCESSFULLY("template.updated.successfully"),
    TEMPLATE_DELETED_SUCCESSFULLY("template.deleted.successfully"),
    TEMPLATE_INVALID_FILTER_REQUEST("template.invalid.filter.request");

    private final String code;
}
