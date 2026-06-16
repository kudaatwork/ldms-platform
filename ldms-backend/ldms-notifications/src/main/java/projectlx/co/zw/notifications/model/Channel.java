package projectlx.co.zw.notifications.model;

public enum Channel {

    EMAIL("EMAIL"),
    SMS("SMS"),
    WHATSAPP("WHATSAPP"),
    IN_APP("IN_APP"),
    SLACK("SLACK"),
    TEAMS("TEAMS");

    private final String channel;

    Channel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}
