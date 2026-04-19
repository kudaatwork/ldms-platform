package projectlx.co.zw.organizationmanagement.utils.exceptions;

import lombok.Getter;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final I18Code i18Code;

    public BusinessRuleException(String message) {
        super(message);
        this.i18Code = null;
    }

    public BusinessRuleException(String message, I18Code i18Code) {
        super(message);
        this.i18Code = i18Code;
    }
}
