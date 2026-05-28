package projectlx.co.zw.notifications.utils.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectLxBrandedEmailHtmlTest {

    @Test
    void contactPersonVerification_containsBrandedLayout() {
        String html = ProjectLxBrandedEmailHtml.contactPersonVerification();
        assertTrue(html.contains("Project LX"));
        assertTrue(html.contains("{{verificationLink}}"));
        assertTrue(html.contains("Verify My Email"));
    }

    @Test
    void kycTemplates_containCtaAndOrganisation() {
        String stage1 = ProjectLxBrandedEmailHtml.kycStage1Approved();
        assertTrue(stage1.contains("KYC STAGE 1 APPROVED"));
        assertTrue(stage1.contains("{{nextStepsLink}}"));

        String stage2 = ProjectLxBrandedEmailHtml.kycStage2Approved();
        assertTrue(stage2.contains("ORGANISATION VERIFIED"));
        assertTrue(stage2.contains("{{signInLink}}"));
    }

    @Test
    void registrationTemplates_useBrandedLayout() {
        String signup = ProjectLxBrandedEmailHtml.organizationSignupReceived();
        assertTrue(signup.contains("REGISTRATION RECEIVED"));
        assertTrue(signup.contains("{{nextStepsLink}}"));

        String admin = ProjectLxBrandedEmailHtml.organizationRegisteredByAdmin();
        assertTrue(admin.contains("ORGANISATION REGISTERED"));
        assertTrue(admin.contains("linear-gradient(135deg,#0f1c45"));
    }
}
