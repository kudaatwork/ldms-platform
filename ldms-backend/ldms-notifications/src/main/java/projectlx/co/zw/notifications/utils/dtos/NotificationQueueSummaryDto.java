package projectlx.co.zw.notifications.utils.dtos;

public class NotificationQueueSummaryDto {

    private String queueName;
    private int messagesReady;
    private int messagesUnacked;
    private String exchangeName;
    private String routingKey;

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }

    public int getMessagesReady() { return messagesReady; }
    public void setMessagesReady(int messagesReady) { this.messagesReady = messagesReady; }

    public int getMessagesUnacked() { return messagesUnacked; }
    public void setMessagesUnacked(int messagesUnacked) { this.messagesUnacked = messagesUnacked; }

    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    @Override
    public String toString() {
        return "NotificationQueueSummaryDto{queueName='" + queueName + "', messagesReady=" + messagesReady
                + ", messagesUnacked=" + messagesUnacked + "}";
    }
}
