package projectlx.co.zw.shared_library.utils.responses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class FileUploadResponse extends CommonResponse {

    private FileUploadDto fileUploadDto;
    private List<FileUploadDto> fileUploadDtoList;
    private Page<FileUploadDto> fileUploadDtoPage;
}
