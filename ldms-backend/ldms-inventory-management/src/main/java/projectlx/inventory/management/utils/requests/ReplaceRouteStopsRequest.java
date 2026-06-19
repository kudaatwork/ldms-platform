package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.RouteStopContextType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class ReplaceRouteStopsRequest {

    @NotNull
    private RouteStopContextType contextType;

    @NotNull
    private Long contextId;

    @NotNull
    private Long organizationId;

    private List<RouteStopRequest> stops = new ArrayList<>();
}
