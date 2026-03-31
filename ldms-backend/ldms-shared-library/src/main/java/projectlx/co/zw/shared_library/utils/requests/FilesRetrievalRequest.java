package projectlx.co.zw.shared_library.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class FilesRetrievalRequest {
    private Long ownerId;
    private String ownerType;
}
