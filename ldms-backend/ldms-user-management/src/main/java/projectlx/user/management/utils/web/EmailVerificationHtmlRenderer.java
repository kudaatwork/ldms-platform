package projectlx.user.management.utils.web;

import projectlx.user.management.utils.responses.UserResponse;

/**
 * Browser HTML for email verification links (GET). Styled to align with Project LX admin auth UI.
 */
public final class EmailVerificationHtmlRenderer {

    /** Matches admin sign-in left panel ({@code login.component.scss}). */
    private static final String BRAND_PANEL_DARK = "#0f1629";
    private static final String BRAND_PANEL_MID = "#1a2650";
    private static final String BRAND_CTA = "#2563eb";
    private static final String BRAND_CTA_HOVER = "#1d4ed8";
    private static final String SUCCESS = "#22c55e";
    private static final String SUCCESS_DARK = "#15803d";
    private static final String ERROR = "#ef4444";
    private static final String ERROR_DARK = "#b91c1c";
    private static final String INFO = "#2563eb";
    private static final String INFO_DARK = "#1d4ed8";

    private EmailVerificationHtmlRenderer() {
    }

    public static String render(UserResponse response, String signInUrl) {
        boolean success = response != null && response.isSuccess();
        boolean alreadyVerified = response != null
                && "ALREADY_VERIFIED".equalsIgnoreCase(response.getEmailVerificationOutcome());
        String pageTitle = alreadyVerified
                ? "Already verified"
                : (success ? "Email verified" : "Verification failed");
        String headline = alreadyVerified
                ? "Account already verified"
                : (success ? "Email verified successfully" : "Verification could not be completed");
        String message = response != null && response.getMessage() != null && !response.getMessage().isBlank()
                ? escapeHtml(response.getMessage())
                : (alreadyVerified
                        ? "This email address is already verified."
                        : (success ? "Your email has been verified." : "We could not verify your email."));
        String hint = alreadyVerified
                ? "You can sign in to the Project LX admin console — no further action is needed."
                : (success
                        ? "You can close this tab and sign in to the Project LX admin console."
                        : "Request a new verification link or contact your administrator.");
        String stateClass = alreadyVerified ? "state-info" : (success ? "state-success" : "state-error");
        String icon = alreadyVerified ? "i" : (success ? "&#10003;" : "!");
        String loginUrl = signInUrl == null || signInUrl.isBlank()
                ? "http://localhost:4200/auth/login"
                : signInUrl;

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>%s | Project LX</title>
                  <style>
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      font-family: "Segoe UI", system-ui, -apple-system, sans-serif;
                      background: linear-gradient(145deg, #eef2ff 0%%, #f8fafc 42%%, #f1f5f9 100%%);
                      color: #0f172a;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      padding: 24px 16px;
                    }
                    .shell {
                      width: 100%%;
                      max-width: 440px;
                      background: #fff;
                      border-radius: 16px;
                      border: 1px solid #e2e8f0;
                      box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
                      overflow: hidden;
                    }
                    .brand {
                      padding: 22px 28px 18px;
                      background: linear-gradient(135deg, %s 0%%, %s 100%%);
                      color: #fff;
                    }
                    .brand__name {
                      font-size: 13px;
                      font-weight: 700;
                      letter-spacing: 0.04em;
                      text-transform: uppercase;
                      opacity: 0.92;
                    }
                    .brand__lx { color: #fbbf24; }
                    .body { padding: 24px 28px 28px; }
                    .result {
                      display: flex;
                      gap: 14px;
                      align-items: flex-start;
                      padding: 16px;
                      border-radius: 12px;
                      border: 1px solid transparent;
                      animation: fadeUp 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
                    }
                    .state-success .result {
                      background: linear-gradient(135deg, rgba(34, 197, 94, 0.14) 0%%, rgba(34, 197, 94, 0.06) 100%%);
                      border-color: rgba(34, 197, 94, 0.38);
                    }
                    .state-error .result {
                      background: linear-gradient(135deg, rgba(239, 68, 68, 0.12) 0%%, rgba(239, 68, 68, 0.05) 100%%);
                      border-color: rgba(239, 68, 68, 0.32);
                    }
                    .state-info .result {
                      background: linear-gradient(135deg, rgba(37, 99, 235, 0.14) 0%%, rgba(37, 99, 235, 0.06) 100%%);
                      border-color: rgba(37, 99, 235, 0.35);
                    }
                    .icon {
                      flex-shrink: 0;
                      width: 44px;
                      height: 44px;
                      border-radius: 999px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      font-size: 22px;
                      font-weight: 700;
                      color: #fff;
                    }
                    .state-success .icon {
                      background: linear-gradient(145deg, %s 0%%, #16a34a 100%%);
                      box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.22), 0 8px 18px rgba(34, 197, 94, 0.35);
                    }
                    .state-error .icon {
                      background: linear-gradient(145deg, %s 0%%, #dc2626 100%%);
                      box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.18);
                    }
                    .state-info .icon {
                      background: linear-gradient(145deg, %s 0%%, %s 100%%);
                      box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.2);
                    }
                    h1 {
                      margin: 0 0 6px;
                      font-size: 17px;
                      font-weight: 700;
                      letter-spacing: -0.02em;
                      line-height: 1.3;
                    }
                    .state-success h1 { color: %s; }
                    .state-error h1 { color: %s; }
                    .state-info h1 { color: %s; }
                    .message {
                      margin: 0;
                      font-size: 14px;
                      line-height: 1.5;
                      font-weight: 500;
                      color: #334155;
                    }
                    .hint {
                      margin: 16px 0 0;
                      font-size: 13px;
                      line-height: 1.45;
                      color: #64748b;
                      font-weight: 500;
                    }
                    .cta {
                      display: inline-flex;
                      align-items: center;
                      justify-content: center;
                      width: 100%%;
                      margin-top: 20px;
                      padding: 13px 16px;
                      border-radius: 10px;
                      border: none;
                      background: %s;
                      color: #fff;
                      font-size: 14px;
                      font-weight: 600;
                      text-decoration: none;
                      transition: background 0.15s ease;
                    }
                    .cta:hover { background: %s; }
                    @keyframes fadeUp {
                      from { opacity: 0; transform: translateY(10px); }
                      to { opacity: 1; transform: translateY(0); }
                    }
                  </style>
                </head>
                <body class="%s">
                  <div class="shell">
                    <div class="brand">
                      <div class="brand__name">Project <span class="brand__lx">LX</span> · Admin Console</div>
                    </div>
                    <div class="body">
                      <div class="result">
                        <div class="icon">%s</div>
                        <div>
                          <h1>%s</h1>
                          <p class="message">%s</p>
                        </div>
                      </div>
                      <p class="hint">%s</p>
                      <a class="cta" href="%s">Sign in to admin console</a>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                pageTitle,
                BRAND_PANEL_DARK,
                BRAND_PANEL_MID,
                SUCCESS,
                ERROR,
                INFO,
                INFO_DARK,
                SUCCESS_DARK,
                ERROR_DARK,
                INFO_DARK,
                BRAND_CTA,
                BRAND_CTA_HOVER,
                stateClass,
                icon,
                headline,
                message,
                hint,
                loginUrl);
    }

    private static String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
