package projectlx.inventory.management.utils.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessageEnvelope {
    private String exchange;
    private String routingKey;
    private Object message;
}
