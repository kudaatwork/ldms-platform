package projectlx.co.zw.shared_library.utils.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorDto {

    private Boolean success;
    private String data;
    private List<String> errorMessages;
}
