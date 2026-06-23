package projectlx.messaging.inbound.repository.projection;

public interface BotTopicCountProjection {

    String getTopic();

    Long getTopicCount();
}
