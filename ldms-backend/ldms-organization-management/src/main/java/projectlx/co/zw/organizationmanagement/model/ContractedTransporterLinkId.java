package projectlx.co.zw.organizationmanagement.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ContractedTransporterLinkId implements Serializable {

    private Long organizationId;
    private Long transporterId;
}
