package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripMessageDto {
    private Long id;
    private Long senderUserId;
    private String senderRole;
    private String senderName;
    private String body;
    private String createdAt;
    private String createdAtLabel;
    private boolean mine;
    private boolean read;
}
