package projectlx.co.zw.shared_library.utils.security;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.util.StringUtils;

import java.util.Base64;

/**
 * RFC 6238 TOTP helpers for authenticator-app 2FA (Google Authenticator, Authy, etc.).
 */
public final class TotpSupport {

    private static final SecretGenerator SECRET_GENERATOR = new DefaultSecretGenerator(32);
    private static final CodeVerifier CODE_VERIFIER =
            new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());
    private static final QrGenerator QR_GENERATOR = new ZxingPngQrGenerator();

    private TotpSupport() {
    }

    public static String generateSecret() {
        return SECRET_GENERATOR.generate();
    }

    public static QrData buildQrData(String secret, String accountName, String issuer) {
        return new QrData.Builder()
                .label(sanitizeLabel(accountName))
                .secret(secret)
                .issuer(sanitizeLabel(issuer))
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
    }

    public static String buildOtpAuthUri(String secret, String accountName, String issuer) {
        return buildQrData(secret, accountName, issuer).getUri();
    }

    public static String qrCodeDataUrl(QrData data) {
        if (data == null) {
            return null;
        }
        try {
            byte[] imageData = QR_GENERATOR.generate(data);
            String mimeType = QR_GENERATOR.getImageMimeType();
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException ex) {
            return null;
        }
    }

    public static boolean verifyCode(String secret, String code) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
            return false;
        }
        String normalized = code.trim().replaceAll("\\s+", "");
        if (!normalized.matches("\\d{6}")) {
            return false;
        }
        return CODE_VERIFIER.isValidCode(secret.trim(), normalized);
    }

    private static String sanitizeLabel(String value) {
        if (!StringUtils.hasText(value)) {
            return "LDMS";
        }
        return value.trim().replace(':', ' ');
    }
}
