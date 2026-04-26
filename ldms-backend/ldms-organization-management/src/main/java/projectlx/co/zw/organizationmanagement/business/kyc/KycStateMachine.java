package projectlx.co.zw.organizationmanagement.business.kyc;

import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.Locale;
import java.util.Set;

/**
 * Allowed KYC transitions only — all other pairs throw {@link BusinessRuleException}.
 */
public class KycStateMachine {

    private static final Set<Transition> ALLOWED = Set.of(
            new Transition(KycStatus.DRAFT, KycStatus.SUBMITTED),
            new Transition(KycStatus.RESUBMITTED, KycStatus.SUBMITTED),
            new Transition(KycStatus.SUBMITTED, KycStatus.STAGE_1_REVIEW),
            new Transition(KycStatus.STAGE_1_REVIEW, KycStatus.STAGE_2_REVIEW),
            new Transition(KycStatus.STAGE_1_REVIEW, KycStatus.REJECTED),
            new Transition(KycStatus.STAGE_2_REVIEW, KycStatus.APPROVED),
            new Transition(KycStatus.STAGE_2_REVIEW, KycStatus.REJECTED),
            new Transition(KycStatus.REJECTED, KycStatus.RESUBMITTED)
    );

    private final MessageService messageService;

    public KycStateMachine(MessageService messageService) {
        this.messageService = messageService;
    }

    public void assertCanTransition(KycStatus from, KycStatus to) {
        assertCanTransition(from, to, Locale.getDefault());
    }

    public void assertCanTransition(KycStatus from, KycStatus to, Locale locale) {
        if (from == null || to == null) {
            throw new BusinessRuleException(
                    messageService.getMessage(I18Code.KYC_INVALID_TRANSITION.getCode(), new String[]{}, locale),
                    I18Code.KYC_INVALID_TRANSITION);
        }
        for (Transition t : ALLOWED) {
            if (t.from == from && t.to == to) {
                return;
            }
        }
        throw new BusinessRuleException(
                messageService.getMessage(
                        I18Code.KYC_INVALID_TRANSITION.getCode(),
                        new String[] { String.valueOf(from), String.valueOf(to) },
                        locale),
                I18Code.KYC_INVALID_TRANSITION);
    }

    private static final class Transition {
        private final KycStatus from;
        private final KycStatus to;

        private Transition(KycStatus from, KycStatus to) {
            this.from = from;
            this.to = to;
        }
    }
}
