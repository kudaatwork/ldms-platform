package projectlx.co.zw.shared_library.billing;

/**
 * Platform wallet action codes aligned with {@code platform_action_charge} catalog.
 */
public final class PlatformWalletActionCodes {

    private PlatformWalletActionCodes() {
    }

    public static final String TRIP_CREATE = "TRIP_CREATE";
    public static final String TRIP_COMPLETE = "TRIP_COMPLETE";
    public static final String TRIP_TRACK = "TRIP_TRACK";
    public static final String TRIP_ASSIGN_DRIVER = "TRIP_ASSIGN_DRIVER";
    public static final String LIVE_MAP_SESSION = "LIVE_MAP_SESSION";
    public static final String GPS_PING = "GPS_PING";

    public static final String SHIPMENT_UPDATE = "SHIPMENT_UPDATE";
    public static final String SHIPMENT_DISPATCH = "SHIPMENT_DISPATCH";

    public static final String ORDER_CREATE = "ORDER_CREATE";
    public static final String INVENTORY_GRV_CREATE = "INVENTORY_GRV_CREATE";
    public static final String INVENTORY_STOCK_RESERVE = "INVENTORY_STOCK_RESERVE";

    public static final String FLEET_VEHICLE_REGISTER = "FLEET_VEHICLE_REGISTER";
    public static final String FLEET_COMPLIANCE_UPLOAD = "FLEET_COMPLIANCE_UPLOAD";
    public static final String FLEET_DRIVER_HIRE = "FLEET_DRIVER_HIRE";

    public static final String NOTIFICATION_EMAIL = "NOTIFICATION_EMAIL";
    public static final String NOTIFICATION_SMS = "NOTIFICATION_SMS";
    public static final String WHATSAPP_COMMAND = "WHATSAPP_COMMAND";

    public static final String HELP_BOT_MESSAGE = "HELP_BOT_MESSAGE";
    public static final String HELP_SUPPORT_TICKET_OPEN = "HELP_SUPPORT_TICKET_OPEN";
    public static final String HELP_LIVE_CHAT_MESSAGE = "HELP_LIVE_CHAT_MESSAGE";

    public static final String INVOICE_GENERATE = "INVOICE_GENERATE";
}
