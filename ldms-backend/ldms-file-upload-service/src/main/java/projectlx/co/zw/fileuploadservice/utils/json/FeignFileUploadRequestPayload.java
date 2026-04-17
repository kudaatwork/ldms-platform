package projectlx.co.zw.fileuploadservice.utils.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeignFileUploadRequestPayload {

    private List<FeignMultipartFileMetadataEntry> filesMetadata;
    private String ownerType;
    private Long ownerId;
}
