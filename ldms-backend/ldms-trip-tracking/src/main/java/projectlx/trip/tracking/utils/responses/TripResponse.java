package projectlx.trip.tracking.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.trip.tracking.utils.dtos.TripDto;
import projectlx.trip.tracking.utils.dtos.TripEventDto;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripResponse extends CommonResponse {

    private TripDto tripDto;
    private List<TripDto> tripDtoList;
    private TripEventDto tripEventDto;
    private List<TripEventDto> tripEventDtoList;
    private TripLiveSnapshotDto liveSnapshot;
}
