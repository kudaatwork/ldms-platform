package projectlx.trip.tracking.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.trip.tracking.utils.dtos.ReceiverContactDto;
import projectlx.trip.tracking.utils.dtos.TripMessageDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripChatResponse extends CommonResponse {
    private List<TripMessageDto> messages;
    private ReceiverContactDto receiverContact;
    private String myRole;
    private Long currentUserId;
    private long unreadCount;
}
