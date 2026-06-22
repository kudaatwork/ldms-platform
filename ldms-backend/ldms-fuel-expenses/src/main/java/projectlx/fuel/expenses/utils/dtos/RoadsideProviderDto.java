package projectlx.fuel.expenses.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.fuel.expenses.model.RoadsideProviderType;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadsideProviderDto {
    private Long id;
    private RoadsideProviderType providerType;
    private String providerTypeLabel;
    private String name;
    private String description;
    private String phone;
    private String servicesOffered;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressLabel;
    private boolean open24Hours;
    private boolean verified;
    private Double distanceKm;
}
