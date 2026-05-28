package projectlx.co.zw.notifications.utils.email;

import java.util.ArrayList;
import java.util.List;

/**
 * Project LX branded transactional email HTML (table layout for email clients).
 * Used by Flyway seeds and kept in sync with {@code db/email-templates/*.html}.
 */
public final class ProjectLxBrandedEmailHtml {

    private ProjectLxBrandedEmailHtml() {
    }

    public static String contactPersonVerification() {
        return build(BrandedEmailSpec.builder()
                .badgeLabel("&#128231; VERIFY YOUR EMAIL")
                .badgeBackground("#eff6ff")
                .badgeBorder("#bfdbfe")
                .badgeColor("#1e40af")
                .greetingNameToken("firstName")
                .bodyHtml("""
                        <p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">
                          You have been listed as the contact person for
                          <strong style="color:#1b2a5e">{{organizationName}}</strong> on Project LX LDMS.
                          Verify your email to activate your user account and access your organisation portal.
                        </p>
                        """)
                .infoRow("Organisation", "{{organizationName}}")
                .infoRow("Email", "{{Email}}")
                .ctaLabel("Verify My Email &rarr;")
                .ctaHref("{{verificationLink}}")
                .warningTitle("Did not expect this?")
                .warningBody(
                        "If you were not named as a contact person for this organisation, you can ignore this message. "
                                + "No account changes will be made unless you verify.")
                .fallbackLinkToken("verificationLink")
                .build());
    }

    public static String kycStage1Approved() {
        return build(BrandedEmailSpec.builder()
                .badgeLabel("&#10003; KYC STAGE 1 APPROVED")
                .badgeBackground("#ecfdf5")
                .badgeBorder("#a7f3d0")
                .badgeColor("#065f46")
                .greetingNameToken("contactName")
                .bodyHtml("""
                        <p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">
                          Good news: the KYC review for
                          <strong style="color:#1b2a5e">{{organizationName}}</strong> has completed
                          <strong style="color:#1b2a5e">Stage 1</strong> successfully. Your application is now in
                          <strong style="color:#047857">Stage 2 review</strong>. We will notify you when the next
                          decision is made.
                        </p>
                        """)
                .infoRow("Organisation", "{{organizationName}}")
                .infoRow("KYC status", "Stage 2 review")
                .ctaLabel("View application &rarr;")
                .ctaHref("{{nextStepsLink}}")
                .fallbackLinkToken("nextStepsLink")
                .build());
    }

    public static String organizationSignupReceived() {
        return build(BrandedEmailSpec.builder()
                .badgeLabel("&#128233; REGISTRATION RECEIVED")
                .badgeBackground("#eff6ff")
                .badgeBorder("#bfdbfe")
                .badgeColor("#1e40af")
                .greetingNameToken("contactName")
                .bodyHtml("""
                        <p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">
                          Thank you for registering
                          <strong style="color:#1b2a5e">{{organizationName}}</strong> on Project LX LDMS.
                          Complete your application and submit KYC when you are ready. We will guide you through the
                          next steps on the platform.
                        </p>
                        """)
                .infoRow("Organisation", "{{organizationName}}")
                .infoRow("Email", "{{Email}}")
                .ctaLabel("Continue application &rarr;")
                .ctaHref("{{nextStepsLink}}")
                .fallbackLinkToken("signInLink")
                .fallbackLinkHint("Sign in later:")
                .build());
    }

    public static String organizationRegisteredByAdmin() {
        return build(BrandedEmailSpec.builder()
                .badgeLabel("&#9989; ORGANISATION REGISTERED")
                .badgeBackground("#ecfdf5")
                .badgeBorder("#a7f3d0")
                .badgeColor("#065f46")
                .greetingNameToken("contactName")
                .bodyHtml("""
                        <p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">
                          Organisation <strong style="color:#1b2a5e">{{organizationName}}</strong> has been registered on
                          Project LX LDMS by an administrator. You can sign in when your portal access is ready.
                        </p>
                        """)
                .infoRow("Organisation", "{{organizationName}}")
                .infoRow("Email", "{{Email}}")
                .ctaLabel("Sign in to Project LX &rarr;")
                .ctaHref("{{signInLink}}")
                .fallbackLinkToken("signInLink")
                .build());
    }

    public static String kycStage2Approved() {
        return build(BrandedEmailSpec.builder()
                .badgeLabel("&#127942; ORGANISATION VERIFIED")
                .badgeBackground("#ecfdf5")
                .badgeBorder("#a7f3d0")
                .badgeColor("#065f46")
                .greetingNameToken("contactName")
                .bodyHtml("""
                        <p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">
                          <strong style="color:#1b2a5e">{{organizationName}}</strong> has completed KYC and is now
                          <strong style="color:#047857">verified</strong> on Project LX LDMS. You can sign in and use
                          the platform.
                        </p>
                        """)
                .infoRow("Organisation", "{{organizationName}}")
                .infoRow("Status", "Verified")
                .ctaLabel("Sign in to Project LX &rarr;")
                .ctaHref("{{signInLink}}")
                .fallbackLinkToken("signInLink")
                .build());
    }

    static String build(BrandedEmailSpec spec) {
        String greetingToken = "{{" + spec.greetingNameToken + "}}";
        String infoRows = renderInfoRows(spec.infoRows);
        String warning = spec.warningTitle != null
                ? renderWarning(spec.warningTitle, spec.warningBody)
                : "";
        String fallbackHint = spec.fallbackLinkHint != null ? spec.fallbackLinkHint : "Trouble with the button? Paste this link into your browser:";

        return """
                <!DOCTYPE html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width,initial-scale=1.0" />
                  </head>
                  <body style="margin:0;padding:0;background:#edf2f7;font-family:Arial,Helvetica,sans-serif">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background:#edf2f7">
                      <tr>
                        <td align="center" style="padding:40px 16px">
                          <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:18px;overflow:hidden">
                            %s
                            %s
                            %s
                            %s
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """
                .formatted(renderHeader(), renderAccentBar(), renderBody(spec, greetingToken, infoRows, warning, fallbackHint), renderFooter());
    }

    private static String renderHeader() {
        return """
                <tr>
                  <td style="background:linear-gradient(135deg,#0f1c45 0%%,#1b2a5e 50%%,#2c3d85 100%%);padding:32px 48px 30px">
                    <table cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="background:#3b82f6;border-radius:10px;padding:10px 16px;font-size:20px;font-weight:800;color:#ffffff;font-family:Arial,sans-serif;letter-spacing:-0.5px">LX</td>
                        <td style="padding-left:16px;vertical-align:middle">
                          <div style="font-size:19px;font-weight:700;color:#ffffff;font-family:Arial,sans-serif">Project LX</div>
                          <div style="font-size:11px;color:rgba(255,255,255,0.48);font-family:Arial,sans-serif;margin-top:3px;letter-spacing:0.2px">Logistics &amp; Distribution Platform</div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
                """;
    }

    private static String renderAccentBar() {
        return """
                <tr>
                  <td height="4" style="background:linear-gradient(90deg,#3b82f6 0%%,#818cf8 48%%,#f97316 100%%);font-size:0;line-height:0">&nbsp;</td>
                </tr>
                """;
    }

    private static String renderBody(
            BrandedEmailSpec spec, String greetingToken, String infoRows, String warning, String fallbackHint) {
        return """
                <tr>
                  <td style="padding:38px 48px 44px">
                    <table cellpadding="0" cellspacing="0" style="margin-bottom:26px">
                      <tr>
                        <td style="background:%s;border:1.5px solid %s;border-radius:20px;padding:6px 16px">
                          <span style="font-size:11px;font-weight:700;color:%s;font-family:Arial,sans-serif;letter-spacing:0.4px">%s</span>
                        </td>
                      </tr>
                    </table>
                    <p style="margin:0 0 14px;font-size:27px;font-weight:700;color:#0f1c45;font-family:Arial,sans-serif;line-height:1.25">Hi %s,</p>
                    %s
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1.5px solid #e2e8f0;border-radius:12px;margin-bottom:30px">
                      <tr><td>%s</td></tr>
                    </table>
                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:26px">
                      <tr>
                        <td align="center">
                          <table cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="background:linear-gradient(135deg,#1b2a5e 0%%,#2c3d85 100%%);border-radius:50px">
                                <a href="%s" style="display:inline-block;padding:16px 52px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;font-family:Arial,sans-serif;letter-spacing:0.3px">%s</a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                    %s
                    <p style="margin:0;font-size:13px;color:#94a3b8;line-height:1.65;font-family:Arial,sans-serif;text-align:center">
                      %s<br /><span style="color:#3b82f6;word-break:break-all;font-size:12px">{{%s}}</span>
                    </p>
                  </td>
                </tr>
                """
                .formatted(
                        spec.badgeBackground,
                        spec.badgeBorder,
                        spec.badgeColor,
                        spec.badgeLabel,
                        greetingToken,
                        spec.bodyHtml,
                        infoRows,
                        spec.ctaHref,
                        spec.ctaLabel,
                        warning,
                        fallbackHint,
                        spec.fallbackLinkToken);
    }

    private static String renderInfoRows(List<InfoRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        for (int i = 0; i < rows.size(); i++) {
            InfoRow row = rows.get(i);
            boolean last = i == rows.size() - 1;
            String border = last ? "" : "border-bottom:1px solid #e2e8f0;";
            sb.append("<tr><td style=\"padding:13px 22px;font-size:13px;color:#64748b;font-family:Arial,sans-serif;")
                    .append(border)
                    .append("\">")
                    .append(row.label())
                    .append("</td><td align=\"right\" style=\"padding:13px 22px;font-size:13px;font-weight:600;color:#0f1c45;font-family:Arial,sans-serif;")
                    .append(border)
                    .append("\">")
                    .append(row.value())
                    .append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private static String renderWarning(String title, String body) {
        return """
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#fef2f2;border:1.5px solid #fecaca;border-radius:12px;margin-bottom:26px">
                  <tr>
                    <td style="padding:18px 22px">
                      <table cellpadding="0" cellspacing="0">
                        <tr>
                          <td style="width:34px;vertical-align:top;padding-top:1px">
                            <div style="width:26px;height:26px;background:#ef4444;border-radius:50%%;text-align:center;line-height:26px;font-size:13px;color:#fff;font-weight:700">!</div>
                          </td>
                          <td style="padding-left:12px">
                            <p style="margin:0 0 4px;font-size:13px;font-weight:700;color:#991b1b;font-family:Arial,sans-serif">%s</p>
                            <p style="margin:0;font-size:13px;color:#b91c1c;line-height:1.6;font-family:Arial,sans-serif">%s</p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
                """
                .formatted(title, body);
    }

    private static String renderFooter() {
        return """
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e8eef4;padding:24px 48px">
                    <p style="margin:0;font-size:11px;color:#94a3b8;text-align:center;line-height:1.75;font-family:Arial,sans-serif">
                      &copy; 2025 Project LX &middot; Logistics &amp; Distribution Management System<br />
                      Sent to <strong>{{Email}}</strong> because it is registered on Project LX.
                    </p>
                  </td>
                </tr>
                """;
    }

    private record InfoRow(String label, String value) {
    }

    static final class BrandedEmailSpec {
        private final String badgeLabel;
        private final String badgeBackground;
        private final String badgeBorder;
        private final String badgeColor;
        private final String greetingNameToken;
        private final String bodyHtml;
        private final List<InfoRow> infoRows;
        private final String ctaLabel;
        private final String ctaHref;
        private final String warningTitle;
        private final String warningBody;
        private final String fallbackLinkToken;
        private final String fallbackLinkHint;

        private BrandedEmailSpec(Builder builder) {
            this.badgeLabel = builder.badgeLabel;
            this.badgeBackground = builder.badgeBackground;
            this.badgeBorder = builder.badgeBorder;
            this.badgeColor = builder.badgeColor;
            this.greetingNameToken = builder.greetingNameToken;
            this.bodyHtml = builder.bodyHtml;
            this.infoRows = List.copyOf(builder.infoRows);
            this.ctaLabel = builder.ctaLabel;
            this.ctaHref = builder.ctaHref;
            this.warningTitle = builder.warningTitle;
            this.warningBody = builder.warningBody;
            this.fallbackLinkToken = builder.fallbackLinkToken;
            this.fallbackLinkHint = builder.fallbackLinkHint;
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private String badgeLabel;
            private String badgeBackground = "#eff6ff";
            private String badgeBorder = "#bfdbfe";
            private String badgeColor = "#1e40af";
            private String greetingNameToken = "firstName";
            private String bodyHtml = "";
            private final List<InfoRow> infoRows = new ArrayList<>();
            private String ctaLabel;
            private String ctaHref;
            private String warningTitle;
            private String warningBody;
            private String fallbackLinkToken;
            private String fallbackLinkHint;

            Builder badgeLabel(String badgeLabel) {
                this.badgeLabel = badgeLabel;
                return this;
            }

            Builder badgeBackground(String badgeBackground) {
                this.badgeBackground = badgeBackground;
                return this;
            }

            Builder badgeBorder(String badgeBorder) {
                this.badgeBorder = badgeBorder;
                return this;
            }

            Builder badgeColor(String badgeColor) {
                this.badgeColor = badgeColor;
                return this;
            }

            Builder greetingNameToken(String greetingNameToken) {
                this.greetingNameToken = greetingNameToken;
                return this;
            }

            Builder bodyHtml(String bodyHtml) {
                this.bodyHtml = bodyHtml;
                return this;
            }

            Builder infoRow(String label, String value) {
                this.infoRows.add(new InfoRow(label, value));
                return this;
            }

            Builder ctaLabel(String ctaLabel) {
                this.ctaLabel = ctaLabel;
                return this;
            }

            Builder ctaHref(String ctaHref) {
                this.ctaHref = ctaHref;
                return this;
            }

            Builder warningTitle(String warningTitle) {
                this.warningTitle = warningTitle;
                return this;
            }

            Builder warningBody(String warningBody) {
                this.warningBody = warningBody;
                return this;
            }

            Builder fallbackLinkToken(String fallbackLinkToken) {
                this.fallbackLinkToken = fallbackLinkToken;
                return this;
            }

            Builder fallbackLinkHint(String fallbackLinkHint) {
                this.fallbackLinkHint = fallbackLinkHint;
                return this;
            }

            BrandedEmailSpec build() {
                return new BrandedEmailSpec(this);
            }
        }
    }
}
