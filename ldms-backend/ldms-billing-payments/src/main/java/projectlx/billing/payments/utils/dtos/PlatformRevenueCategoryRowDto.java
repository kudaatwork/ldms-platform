package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformRevenueCategoryRowDto {

    private String category;
    private long amountCents;
    private String color;
}
