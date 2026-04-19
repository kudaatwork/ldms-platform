package projectlx.co.zw.organizationmanagement.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.organizationmanagement.business.auditable.api.BranchServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.IndustryServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationKycReviewServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.impl.BranchServiceAuditableImpl;
import projectlx.co.zw.organizationmanagement.business.auditable.impl.IndustryServiceAuditableImpl;
import projectlx.co.zw.organizationmanagement.business.auditable.impl.OrganizationKycReviewServiceAuditableImpl;
import projectlx.co.zw.organizationmanagement.business.auditable.impl.OrganizationServiceAuditableImpl;
import projectlx.co.zw.organizationmanagement.business.kyc.KycStateMachine;
import projectlx.co.zw.organizationmanagement.business.kyc.OrganizationEventPublisher;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.business.logic.impl.OrganizationServiceImpl;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.business.validation.impl.OrganizationServiceValidatorImpl;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;
import projectlx.co.zw.organizationmanagement.repository.IndustryRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationKycReviewRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Configuration
public class BusinessConfig {

    @Bean
    public IndustryServiceAuditable industryServiceAuditable(IndustryRepository industryRepository) {
        return new IndustryServiceAuditableImpl(industryRepository);
    }

    @Bean
    public OrganizationServiceAuditable organizationServiceAuditable(OrganizationRepository organizationRepository) {
        return new OrganizationServiceAuditableImpl(organizationRepository);
    }

    @Bean
    public BranchServiceAuditable branchServiceAuditable(BranchRepository branchRepository) {
        return new BranchServiceAuditableImpl(branchRepository);
    }

    @Bean
    public OrganizationKycReviewServiceAuditable organizationKycReviewServiceAuditable(
            OrganizationKycReviewRepository organizationKycReviewRepository) {
        return new OrganizationKycReviewServiceAuditableImpl(organizationKycReviewRepository);
    }

    @Bean
    public OrganizationServiceValidator organizationServiceValidator(MessageService messageService) {
        return new OrganizationServiceValidatorImpl(messageService);
    }

    @Bean
    public KycStateMachine kycStateMachine(MessageService messageService) {
        return new KycStateMachine(messageService);
    }

    @Bean
    public OrganizationEventPublisher organizationEventPublisher(RabbitTemplate rabbitTemplate) {
        return new OrganizationEventPublisher(rabbitTemplate);
    }

    @Bean
    public OrganizationService organizationService(
            OrganizationRepository organizationRepository,
            IndustryRepository industryRepository,
            BranchRepository branchRepository,
            OrganizationKycReviewRepository organizationKycReviewRepository,
            OrganizationServiceAuditable organizationServiceAuditable,
            OrganizationKycReviewServiceAuditable organizationKycReviewServiceAuditable,
            BranchServiceAuditable branchServiceAuditable,
            OrganizationServiceValidator organizationServiceValidator,
            KycStateMachine kycStateMachine,
            OrganizationEventPublisher organizationEventPublisher,
            MessageService messageService) {
        return new OrganizationServiceImpl(
                organizationRepository,
                industryRepository,
                branchRepository,
                organizationKycReviewRepository,
                organizationServiceAuditable,
                organizationKycReviewServiceAuditable,
                branchServiceAuditable,
                organizationServiceValidator,
                kycStateMachine,
                organizationEventPublisher,
                messageService);
    }
}
