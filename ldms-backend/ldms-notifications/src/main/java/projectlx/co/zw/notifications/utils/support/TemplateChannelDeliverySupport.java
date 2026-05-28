package projectlx.co.zw.notifications.utils.support;

import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-channel delivery flags on {@link NotificationTemplate}: channels may be listed for admin UX
 * while only a subset is actually dispatched (organisation templates: email only for now).
 */
public final class TemplateChannelDeliverySupport {

    private TemplateChannelDeliverySupport() {
    }

    public static boolean isOrganizationTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return false;
        }
        String key = templateKey.trim().toUpperCase();
        return key.startsWith("ORG_") || key.startsWith("ORGANIZATION_");
    }

    public static boolean isChannelDeliveryEnabled(NotificationTemplate template, Channel channel) {
        if (template == null || channel == null) {
            return false;
        }
        return isChannelDeliveryEnabled(template.getChannelDeliveryEnabled(), channel);
    }

    public static boolean isChannelDeliveryEnabled(Map<String, Boolean> channelDeliveryEnabled, Channel channel) {
        if (channel == null) {
            return false;
        }
        if (channelDeliveryEnabled == null || channelDeliveryEnabled.isEmpty()) {
            return true;
        }
        Boolean flag = channelDeliveryEnabled.get(channel.name());
        if (flag == null) {
            return true;
        }
        return Boolean.TRUE.equals(flag);
    }

    public static Map<String, Boolean> defaultOrganizationDeliveryFlags() {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put(Channel.EMAIL.name(), true);
        flags.put(Channel.SMS.name(), false);
        flags.put(Channel.WHATSAPP.name(), false);
        return flags;
    }

    public static List<Channel> defaultOrganizationChannels() {
        return List.of(Channel.EMAIL, Channel.SMS, Channel.WHATSAPP);
    }
}
