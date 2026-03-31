package projectlx.co.zw.shared_library.utils.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorDto {

    public Boolean success;
    public Object data;
    public List<String> errorMessages;
}
