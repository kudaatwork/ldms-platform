package projectlx.co.zw.shared_library.utils.dtos;

import java.util.List;

public record ValidatorDto(boolean success, Object data, List<?> errorMessages) {
}
