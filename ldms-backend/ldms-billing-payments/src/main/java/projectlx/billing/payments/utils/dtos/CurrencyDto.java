package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyDto {
    private Long id;
    private String code;
    private String name;
    private String symbol;
    private Integer decimalPlaces;
}
