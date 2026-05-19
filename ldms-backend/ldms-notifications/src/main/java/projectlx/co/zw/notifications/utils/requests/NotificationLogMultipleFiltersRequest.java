package projectlx.co.zw.notifications.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class NotificationLogMultipleFiltersRequest extends MultipleFiltersRequest {

    private String templateKey;
    private String channel;
    private String status;
    /** Case-insensitive contains match on {@code NotificationLog.provider} (e.g. AWS_SES, TWILIO). */
    private String provider;
    private String recipientId;
    /** ISO-8601 date-time (inclusive). */
    private String from;
    /** ISO-8601 date-time (inclusive). */
    private String to;
}
