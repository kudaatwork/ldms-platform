package projectlx.co.zw.fileuploadservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

import java.util.List;

@Getter
@Setter
@ToString
public class FileUploadMultipleFiltersRequest extends MultipleFiltersRequest {

    private String originalFileName;
    /** Case-insensitive contains match on {@link projectlx.co.zw.shared_library.utils.enums.FileType} name. */
    private String fileType;
    /** {@link projectlx.co.zw.shared_library.utils.enums.EntityStatus} name. */
    private String entityStatus;
    /**
     * When non-null, limits ORGANIZATION uploads to these owner ids.
     * An empty list means the caller resolved organisation filters with no matches.
     */
    private List<Long> organizationOwnerIds;
}
