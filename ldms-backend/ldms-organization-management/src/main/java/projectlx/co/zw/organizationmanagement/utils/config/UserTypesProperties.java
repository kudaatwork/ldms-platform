package projectlx.co.zw.organizationmanagement.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.organization")
public class UserTypesProperties {

    private Kyc kyc = new Kyc();

    @Getter
    @Setter
    public static class Kyc {
        /** When false, reviewers must explicitly move SUBMITTED into Stage 1 (not used if stage1 actions accept SUBMITTED). */
        private boolean autoAdvanceToStage1 = false;
    }
}
