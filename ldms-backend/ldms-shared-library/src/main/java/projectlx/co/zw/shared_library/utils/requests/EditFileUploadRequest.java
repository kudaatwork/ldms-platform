package projectlx.co.zw.shared_library.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@ToString
public class EditFileUploadRequest {
    private List<SingleFileUploadRequest> filesMetadata;
    private String ownerType;
    private Long ownerId;
}
