package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class AgentOrganization {
    private Long id;
    private Agent agent;
    private Organization organization;
    private RelationType relationType;
    private LocalDateTime assignedAt;
}