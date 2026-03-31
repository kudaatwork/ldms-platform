package projectlx.co.zw.shared_library.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class MultipleFiltersRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int page;
    private int size;
    private String searchValue;
}
